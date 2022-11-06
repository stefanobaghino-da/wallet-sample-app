package com.daml.wallet;

import com.daml.ledger.api.v1.admin.PartyManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass.AllocatePartyRequest;
import com.daml.ledger.javaapi.data.CreateUserRequest;
import com.daml.ledger.rxjava.DamlLedgerClient;

import io.grpc.ManagedChannel;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

public final class Daml02AllocateParty {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    private final static String DAML_USER = "alice";

    private static String allocateParty() {
        ManagedChannel channel = NettyChannelBuilder
                .forAddress(DAML_HOST, DAML_PORT)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        AllocatePartyRequest request = AllocatePartyRequest.newBuilder().build();
        return PartyManagementServiceGrpc
                .newBlockingStub(channel)
                .allocateParty(request)
                .getPartyDetails()
                .getParty();
    }

    public static void main(String[] args) {
        String party = allocateParty();
        ledger.connect();
        ledger.getUserManagementClient()
                .createUser(new CreateUserRequest(DAML_USER, party))
                .doAfterTerminate(() -> System.exit(0))
                .subscribe(System.out::println);
        Util.waitIndefinitely();
    }

}
