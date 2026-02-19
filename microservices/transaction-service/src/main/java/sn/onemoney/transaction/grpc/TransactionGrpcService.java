package sn.onemoney.transaction.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import sn.onemoney.transaction.model.Transaction;
import sn.onemoney.transaction.service.TransactionService;

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
                .setTimestamp(transaction.getCreatedAt().getTime())
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
}
