package com.daml.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.daml.ledger.api.v1.CompletionOuterClass.Completion;
import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CompletionEndResponse;
import com.daml.ledger.javaapi.data.CompletionStreamResponse;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.rxjava.CommandCompletionClient;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.asset.Asset;
import com.daml.wallet.ledger.asset.AssetType;
import com.daml.wallet.ledger.da.set.types.Set;
import com.google.protobuf.Empty;

import io.reactivex.Single;

public final class Daml10CommandTracking {

    private final static String DAML_HOST = "localhost";
    private final static int DAML_PORT = 6865;
    private final static DamlLedgerClient ledger = DamlLedgerClient.newBuilder(DAML_HOST, DAML_PORT).build();

    private final static String DAML_USER = "alice";

    private final static Single<String> getParty() {
        return ledger.getUserManagementClient()
                .getUser(new GetUserRequest(DAML_USER))
                .map(response -> response.getUser().getPrimaryParty().orElseThrow());
    }

    private final static <T> Set<T> setOf(T item) {
        return new Set<>(Map.of(item, Unit.getInstance()));
    }

    private final static Single<Empty> submitAssetCreation(String party, String commandId) {
        AssetType assetType = new AssetType(party, "ALC", true, Optional.empty());
        CreateCommand create = Asset.create(assetType, party, BigDecimal.valueOf(10.0), setOf(party));
        List<Command> commands = List.of(create);
        String workflowId = "my-workflow";
        return ledger.getCommandSubmissionClient().submit(workflowId, DAML_USER, commandId, party, commands);
    }

    private final static Single<String> listenForCompletion(String party, String commandId) {
        CommandCompletionClient completions = ledger.getCommandCompletionClient();
        return completions
                .completionEnd()
                .map(CompletionEndResponse::getOffset)
                .toFlowable()
                .flatMap(offset -> completions.completionStream(DAML_USER, offset, java.util.Set.of(party)))
                .flatMapIterable(CompletionStreamResponse::getCompletions)
                .map(Completion::getCommandId)
                .filter(completedCommandId -> completedCommandId.equals(commandId))
                .firstOrError();
    }

    public static void main(String[] args) {
        ledger.connect();
        String commandId = UUID.randomUUID().toString();
        getParty()
                .flatMap(party -> Daml10CommandTracking.listenForCompletion(party, commandId))
                .doOnSuccess(completedCommandId -> System.out.println("Asset created by command " + completedCommandId))
                .subscribe(empty -> System.exit(0));
        getParty()
                .flatMap(party -> Daml10CommandTracking.submitAssetCreation(party, commandId))
                .subscribe(empty -> System.out.println("Asset creation submitted as command " + commandId));
        Util.waitIndefinitely();
    }

}
