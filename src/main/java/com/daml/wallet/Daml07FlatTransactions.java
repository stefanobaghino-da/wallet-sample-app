package com.daml.wallet;

import java.util.Map;
import java.util.Set;

import com.daml.ledger.javaapi.data.ArchivedEvent;
import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.Event;
import com.daml.ledger.javaapi.data.Filter;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.InclusiveFilter;
import com.daml.ledger.javaapi.data.LedgerOffset;
import com.daml.ledger.javaapi.data.Transaction;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.asset.Asset;

import io.reactivex.Flowable;
import io.reactivex.Single;

public final class Daml07FlatTransactions {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    private final static String DAML_USER = "alice";

    private final static Single<String> getParty() {
        return ledger.getUserManagementClient()
                .getUser(new GetUserRequest(DAML_USER))
                .map(response -> response.getUser().getPrimaryParty().orElseThrow());
    }

    private final static Flowable<Transaction> getTransactions(String party) {
        LedgerOffset offsetFrom = LedgerOffset.LedgerBegin.getInstance();
        LedgerOffset offsetTo = LedgerOffset.LedgerEnd.getInstance();
        TransactionFilter filter = filterFor(party, Asset.TEMPLATE_ID);
        return ledger.getTransactionsClient().getTransactions(offsetFrom, offsetTo, filter, false);
    }

    private final static TransactionFilter filterFor(String party, Identifier templateId) {
        Filter filter = InclusiveFilter.ofTemplateIds(Set.of(templateId));
        return new FiltersByParty(Map.of(party, filter));
    }

    private final static void printEvents(Transaction transaction) {
        for (Event event : transaction.getEvents()) {
            if (event instanceof CreatedEvent) {
                System.out.println("Created  " + event.getContractId().substring(0, 7));
            } else if (event instanceof ArchivedEvent) {
                System.out.println("Archived " + event.getContractId().substring(0, 7));
            }
        }
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty().toFlowable()
                .flatMap(Daml07FlatTransactions::getTransactions)
                .doOnComplete(() -> System.exit(0))
                .subscribe(Daml07FlatTransactions::printEvents);
        Util.waitIndefinitely();
    }

}
