package com.daml.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.GetUserRequest;
import com.daml.ledger.javaapi.data.Unit;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.wallet.ledger.asset.Asset;
import com.daml.wallet.ledger.asset.AssetType;
import com.daml.wallet.ledger.da.set.types.Set;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.rpc.Status;
import com.google.rpc.ResourceInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.RequestInfo;
import com.google.rpc.RetryInfo;

import io.grpc.protobuf.StatusProto;
import io.reactivex.Single;

public final class Daml11HandlingErrors {

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

    private final static Single<Empty> tryToCreateFaultyAsset(String party) {
        AssetType assetType = new AssetType(party, "ALC", false, Optional.empty());
        CreateCommand create = Asset.create(assetType, party, BigDecimal.valueOf(10.0), setOf(party));
        List<Command> commands = List.of(create);
        String workflowId = "my-workflow";
        String commandId = UUID.randomUUID().toString();
        return ledger.getCommandClient().submitAndWait(workflowId, DAML_USER, commandId, party, commands);
    }

    private final static <T extends Message> Optional<T> cast(Any protobuf, Class<T> tClass) {
        T result = null;
        try {
            if (protobuf.is(tClass)) {
                result = protobuf.unpack(tClass);
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(result);
    }

    private final static void printErrorInfo(ErrorInfo info) {
        System.out.println("  ErrorInfo");
        System.out.println("    Reason: " + info.getReason());
        System.out.println("    ErrorInfoMetadata");
        for (Entry<String, String> metadataEntry: info.getMetadataMap().entrySet()) {
            String key = metadataEntry.getKey();
            String value = metadataEntry.getValue();
            if (value.length() > 40) {
                value = value.substring(0, 37) + "...";
            }
            System.out.println("      " + key + ": " + value);
        }
    }

    private final static void printResourceInfo(ResourceInfo info) {
        System.out.println("  ResourceInfo");
        System.out.println("    Resource type: " + info.getResourceType());
        System.out.println("    Resource name: " + info.getResourceName());
    }

    private final static void printRequestInfo(RequestInfo info) {
        System.out.println("  RequestInfo");
        System.out.println("    Request ID: " + info.getRequestId());
    }

    private final static void printRetryInfo(RetryInfo info) {
        System.out.println("  RetryInfo");
        System.out.println("    Retry delay: " + info.getRetryDelay().getSeconds() + "." + info.getRetryDelay().getNanos());
    }

    private final static void printLedgerApiError(Throwable throwable) {
        Status error = StatusProto.fromThrowable(throwable);
        if (error == null) {
            System.out.println("Not a Ledger API error, printing raw stack trace");
            throwable.printStackTrace();
            return;
        }
        System.out.println("Ledger API error");
        System.out.println("  Code: " + error.getCode());
        for (Any info: error.getDetailsList()) {
            cast(info, ErrorInfo.class).ifPresent(Daml11HandlingErrors::printErrorInfo);
            cast(info, ResourceInfo.class).ifPresent(Daml11HandlingErrors::printResourceInfo);
            cast(info, RequestInfo.class).ifPresent(Daml11HandlingErrors::printRequestInfo);
            cast(info, RetryInfo.class).ifPresent(Daml11HandlingErrors::printRetryInfo);
        }
    }

    public static void main(String[] args) {
        ledger.connect();
        getParty()
                .flatMap(Daml11HandlingErrors::tryToCreateFaultyAsset)
                .doOnSuccess(empty -> System.out.println("Success is not an option!"))
                .doOnError(Daml11HandlingErrors::printLedgerApiError)
                .subscribe(empty -> System.exit(0), throwable -> System.exit(1));
        Util.waitIndefinitely();
    }

}
