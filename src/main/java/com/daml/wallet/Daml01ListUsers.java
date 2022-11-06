package com.daml.wallet;

import com.daml.ledger.javaapi.data.User;
import com.daml.ledger.rxjava.DamlLedgerClient;

public class Daml01ListUsers {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    public static void main(String[] args) {
        ledger.connect();
        ledger.getUserManagementClient().listUsers().subscribe(response -> {
            for (User user: response.getUsers()) {
                System.out.println(user);
            }
            System.exit(0);
        });
        Util.waitIndefinitely();
    }
    
}
