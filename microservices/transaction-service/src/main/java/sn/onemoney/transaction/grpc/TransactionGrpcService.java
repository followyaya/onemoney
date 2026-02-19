package sn.onemoney.transaction.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import sn.onemoney.transaction.model.Transaction;
import sn.onemoney.transaction.service.TransactionService;
import java.time.ZoneId;
import java.util.List;

@GrpcService
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {
    
    @Autowired
    private TransactionService transactionService;
    
    @Override
    public void processPayment(PaymentRequest request,
                              StreamObserver<PaymentResponse> responseObserver) {
        try {
            Transaction transaction = transactionService.processPayment(
                request.getDebitorPhone(),
                request.getCreditorId(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
            );
            
            PaymentResponse response = PaymentResponse.newBuilder()
                .setStatus(transaction.getStatus())
                .setReference(transaction.getReference())
                .setTimestamp(transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .setMessage("Transaction traitée avec succès")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getBalance(BalanceRequest request,
                          StreamObserver<BalanceResponse> responseObserver) {
        try {
            long balance = transactionService.getBalance(request.getPhoneNumber());
            
            BalanceResponse response = BalanceResponse.newBuilder()
                .setPhoneNumber(request.getPhoneNumber())
                .setBalance(balance)
                .setCurrency("XOF")
                .setLastUpdated(System.currentTimeMillis())
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void streamTransactions(TransactionStreamRequest request,
                                   StreamObserver<sn.onemoney.transaction.grpc.Transaction> responseObserver) {
        try {
            List<Transaction> list = transactionService.streamTransactions(request.getPhoneNumber(), request.getLimit());
            for (Transaction t : list) {
                sn.onemoney.transaction.grpc.Transaction msg = sn.onemoney.transaction.grpc.Transaction.newBuilder()
                        .setId(t.getId() == null ? "" : t.getId())
                        .setType("PAYMENT")
                        .setAmount(t.getAmount() == null ? 0L : t.getAmount())
                        .setStatus(t.getStatus() == null ? "" : t.getStatus())
                        .setTimestamp(t.getCreatedAt() == null ? System.currentTimeMillis() : t.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        .setCounterparty(t.getCreditorId() == null ? "" : t.getCreditorId())
                        .build();
                responseObserver.onNext(msg);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
