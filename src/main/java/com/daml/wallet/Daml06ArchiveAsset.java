package com.daml.wallet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Filter;
import com.daml.ledger.javaapi.data.FiltersByParty;
import com.daml.ledger.javaapi.data.GetActiveContractsResponse;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.InclusiveFilter;
import com.daml.ledger.javaapi.data.TransactionFilter;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.Decoder;
import com.daml.wallet.ledger.asset.Asset;
import com.daml.wallet.ledger.da.internal.template.Archive;
import com.google.protobuf.Empty;

import io.reactivex.Flowable;
import io.reactivex.Single;

public final class Daml06ArchiveAsset {

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

    private final static Single<Empty> archiveAsset(Asset.Contract asset) {
        ExerciseCommand exercise = asset.id.exerciseArchive(new Archive());
        System.out.println("Archiving contract with ID " + asset.id.contractId.substring(0, 7));
        List<Command> commands = List.of(exercise);
        String workflowId = "my-workflow";
        String commandId = UUID.randomUUID().toString();
        String party = asset.signatories.iterator().next();
        return ledger.getCommandClient().submitAndWait(workflowId, DAML_USER, commandId, party, commands);
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty().toFlowable()
                .flatMap(Daml06ArchiveAsset::getActiveContracts)
                .flatMapIterable(GetActiveContractsResponse::getCreatedEvents)
                .map(Decoder::fromCreatedEvent)
                .cast(Asset.Contract.class)
                .firstOrError()
                .map(Daml06ArchiveAsset::archiveAsset)
                .subscribe(empty -> System.exit(0));
        Util.waitIndefinitely();
    }

}
