from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Dict
import json
import asyncio
from datetime import datetime
import redis
import os
from jose import jwt, JWTError

app = FastAPI(title="OneMoney Notification Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

redis_client = redis.Redis(
    host=os.getenv("REDIS_HOST", "localhost"),
    port=int(os.getenv("REDIS_PORT", 6379)),
    decode_responses=True
)

class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}
    
    async def connect(self, websocket: WebSocket, user_id: str):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
    
    def disconnect(self, websocket: WebSocket, user_id: str):
        if user_id in self.active_connections:
            self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
    
    async def send_personal_message(self, message: dict, user_id: str):
        if user_id in self.active_connections:
            for connection in self.active_connections[user_id]:
                try:
                    await connection.send_json(message)
                except:
                    pass
    
    async def broadcast(self, message: dict):
        for user_id in self.active_connections:
            await self.send_personal_message(message, user_id)

manager = ConnectionManager()

@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    await manager.connect(websocket, user_id)
    try:
        await websocket.send_json({
            "type": "connection_established",
            "user_id": user_id,
            "timestamp": datetime.now().isoformat()
        })
        
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            
            if message.get("type") == "ping":
                await websocket.send_json({"type": "pong"})
            
            elif message.get("type") == "pin_confirm":
                transaction_id = message.get("transactionId")
                pin = message.get("pin")
                
                redis_client.setex(
                    f"pin:{transaction_id}",
                    300,
                    pin
                )
                
                await websocket.send_json({
                    "type": "pin_received",
                    "transaction_id": transaction_id,
                    "status": "processing"
                })
    
    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
        await manager.broadcast({
            "type": "user_disconnected",
            "user_id": user_id
        })

@app.post("/api/v1/notify/{user_id}")
async def notify_user(user_id: str, notification: dict):
    await manager.send_personal_message(notification, user_id)
    return {"status": "sent", "user_id": user_id}

@app.post("/api/v1/broadcast")
async def broadcast_notification(notification: dict):
    await manager.broadcast(notification)
    return {"status": "broadcasted"}

@app.get("/api/v1/health")
async def health_check():
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "connections": sum(len(conns) for conns in manager.active_connections.values())
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
