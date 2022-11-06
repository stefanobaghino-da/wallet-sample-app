package com.daml.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.asset.Asset;
import com.daml.wallet.ledger.asset.AssetType;
import com.daml.wallet.ledger.da.set.types.Set;
import com.google.protobuf.Empty;

import io.reactivex.Single;

public final class Daml04CreateAsset {

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

    private final static Single<Empty> createAsset(String party) {
        AssetType assetType = new AssetType(party, "ALC", true, Optional.empty());
        CreateCommand create = Asset.create(assetType, party, BigDecimal.valueOf(10.0), setOf(party));
        List<Command> commands = List.of(create);
        String workflowId = "my-workflow";
        String commandId = UUID.randomUUID().toString();
        return ledger.getCommandClient().submitAndWait(workflowId, DAML_USER, commandId, party, commands);
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty()
                .flatMap(Daml04CreateAsset::createAsset)
                .doOnSuccess(empty -> System.out.println("Asset created!"))
                .subscribe(empty -> System.exit(0));
        Util.waitIndefinitely();
    }

}
