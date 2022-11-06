package com.daml.wallet;

import java.util.Map;
import java.util.Set;

import com.daml.ledger.javaapi.data.Filter;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.GetActiveContractsResponse;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.InclusiveFilter;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.asset.Asset;

import io.reactivex.Flowable;
import io.reactivex.Single;

public final class Daml03GetActiveContracts {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    private final static String DAML_USER = "alice";

    private final static Single<String> getParty() {
        return ledger.getUserManagementClient()
                .getUser(new GetUserRequest(DAML_USER))
                .map(response -> response.getUser().getPrimaryParty().orElseThrow());
    }

    private final static Flowable<GetActiveContractsResponse> getActiveContracts(String party) {
        return ledger.getActiveContractSetClient().getActiveContracts(filterFor(party, Asset.TEMPLATE_ID), false);
    }

    private final static TransactionFilter filterFor(String party, Identifier templateId) {
        Filter filter = InclusiveFilter.ofTemplateIds(Set.of(templateId));
        return new FiltersByParty(Map.of(party, filter));
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty().toFlowable()
                .flatMap(Daml03GetActiveContracts::getActiveContracts)
                .flatMapIterable(response -> response.getCreatedEvents())
                .doOnComplete(() -> System.exit(0))
                .subscribe(System.out::println);
        Util.waitIndefinitely();
    }

}
