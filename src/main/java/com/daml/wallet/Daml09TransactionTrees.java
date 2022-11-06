package com.daml.wallet;

import java.util.List;
import java.util.Map;

import com.daml.ledger.javaapi.data.CreatedEvent;
import com.daml.ledger.javaapi.data.ExercisedEvent;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.LedgerOffset;
import com.daml.ledger.javaapi.data.NoFilter;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.javaapi.data.TransactionTree;
import com.daml.ledger.javaapi.data.TreeEvent;
import com.daml.ledger.rxjava.DamlLedgerClient;

import io.reactivex.Flowable;
import io.reactivex.Single;

public final class Daml09TransactionTrees {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    private final static String DAML_USER = "alice";

    private final static Single<String> getParty() {
        return ledger.getUserManagementClient()
                .getUser(new GetUserRequest(DAML_USER))
                .map(response -> response.getUser().getPrimaryParty().orElseThrow());
    }

    private final static Flowable<TransactionTree> getTransactions(String party) {
        LedgerOffset offsetFrom = LedgerOffset.LedgerBegin.getInstance();
        LedgerOffset offsetTo = LedgerOffset.LedgerEnd.getInstance();
        TransactionFilter filter = filterFor(party);        
        return ledger.getTransactionsClient().getTransactionsTrees(offsetFrom, offsetTo, filter, false);
    }

    private final static TransactionFilter filterFor(String party) {
        return new FiltersByParty(Map.of(party, NoFilter.instance));
    }

    private final static void printEventAndConsequences(String eventId, Map<String, TreeEvent> eventsById, int nesting) {
        for (int i = 0; i < nesting; i++) {
            System.out.print("  ");
        }
        TreeEvent event = eventsById.get(eventId);
        System.out.print(event.getContractId().substring(0, 7) + " ");
        if (event instanceof CreatedEvent) {
            CreatedEvent created = (CreatedEvent)event;
            System.out.println(created.getTemplateId().getEntityName() + " created");
        } else if (event instanceof ExercisedEvent) {
            ExercisedEvent exercised = (ExercisedEvent)event;
            System.out.println(exercised.getTemplateId().getEntityName() + (exercised.isConsuming() ? " consumed by " : " exercised ") + exercised.getChoice());
            for (String childEventId: exercised.getChildEventIds()) {
                printEventAndConsequences(childEventId, eventsById, nesting + 1);
            }
        }
    }

    private final static void printTransaction(TransactionTree transaction) {
        System.out.println("Transaction " + transaction.getTransactionId());
        List<String> rootIds = transaction.getRootEventIds();
        Map<String, TreeEvent> eventsById = transaction.getEventsById();
        for (String rootId: rootIds) {
            printEventAndConsequences(rootId, eventsById, 1);
        }
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty().toFlowable()
                .flatMap(Daml09TransactionTrees::getTransactions)
                .doOnComplete(() -> System.exit(0))
                .subscribe(Daml09TransactionTrees::printTransaction);
        Util.waitIndefinitely();
    }

}
