# HTTP JSON API Service Walkthrough

## Start all the necessary services

Open a terminal and build the application by running:

```
.\build.ps1
```

Now you can start the sandbox with the following command:

```
daml sandbox --dar main/Asset/asset.dar --dar main/User/user.dar --dar main/Account/account.dar
```

In a separate terminal, run:

```
daml json-api --ledger-host localhost --ledger-port 6865 --http-port 7575
```

## Useful links

- Endpoints:             https://docs.daml.com/json-api/index.html#create-a-new-contract
- Search query language: https://docs.daml.com/json-api/search-query-language.html
- Daml-LF JSON encoding: https://docs.daml.com/json-api/lf-value-specification.html

## Useful resources

To get the expected Daml-LF JSON encoding of a Daml value, you can use `daml repl` like to:

```
daml> import qualified DA.Set as Set
daml> :json Set.fromList [1,2,3]
{"map":[[1,{}],[2,{}],[3,{}]]}
```

# Trigger Service Walkthrough

## Set up the environment

If you still need to build the application, first run:

```
.\build.ps1
```

If you still have a running sandbox and HTTP JSON API service, remember to shut them down to
start from a clean slate.

Now start the sandbox with the same command as before:

```
daml sandbox --dar main/Asset/asset.dar --dar main/User/user.dar --dar main/Account/account.dar
```

You will need to also run the setup script as follows in a separate terminal:

```
daml script --dar ./main/Account/account.dar --script-name Setup:setup --ledger-host localhost --ledger-port 6865
```

Finally, start the Trigger Service with the following command:

```
daml trigger-service --ledger-host localhost --ledger-port 6865
```

# Java Bindings Walkthrough

If you still need to build the application, first run:

```
.\build.ps1
```

Now you have to generate the Java code that maps to your Daml models. You can do so by running:

```
daml codegen java `
    -o src/main/java `
    -d com.daml.wallet.ledger.Decoder `
    main/Asset/asset.dar=com.daml.wallet.ledger `
    main/User/user.dar=com.daml.wallet.ledger `
    main/Account/account.dar=com.daml.wallet.ledger
```

Before moving on to the application, let's start the sandbox:

```
daml sandbox --dar main/Asset/asset.dar --dar main/User/user.dar --dar main/Account/account.dar
```
