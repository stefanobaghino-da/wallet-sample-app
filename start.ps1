$CantonPortFile = "./canton-sandbox-port.txt"
$JsonApiPortFile="./json-api-port.txt"

$MaxAttempts = 60

Remove-Item $CantonPortFile
Remove-Item $JsonApiPortFile

$Attempts = 0
Start-Process -NoNewWindow daml -ArgumentList "sandbox --dar main/Asset/asset.dar --dar main/User/user.dar --dar main/Account/account.dar --port-file $CantonPortFile"
while (!(Test-Path $CantonPortFile)) {
  $Attempts = $Attempts + 1
  if ($Attempts -gt $MaxAttempts) {
    Write-Error "Canton doesn't seem to have started, aborting."
    exit 1;
  }
  Start-Sleep 1
}

$Attempts = 0
Start-Process -NoNewWindow daml -ArgumentList "json-api --config json-api-app.conf"
while (!(Test-Path $JsonApiPortFile)) {
  $Attempts = $Attempts + 1
  if ($Attempts -gt $MaxAttempts) {
    Write-Error "The HTTP JSON API service doesn't seem to have started, aborting."
    exit 1;
  }
  Start-Sleep 1
}

Write-Output "Canton and the HTTP JSON API service are running!"

Write-Output "Adding Parties"
daml script --dar ./main/Account/account.dar --script-name Setup:setup --ledger-host localhost --ledger-port 6865
Write-Output "Parties added"

Write-Output "Starting Trigger 1"
Start-Process -NoNewWindow daml -ArgumentList "trigger --dar triggers/triggers.dar --trigger-name SendAssetHoldingAccountInviteTrigger:sendAssetHoldingAccountInviteTrigger --ledger-host localhost --ledger-port 6865 --ledger-user admin"
Start-Sleep 10

Write-Output "Starting Trigger 2"
Start-Process -NoNewWindow daml -ArgumentList "trigger --dar triggers/triggers.dar --trigger-name AcceptAirdropRequestTrigger:acceptAirdropRequestTrigger --ledger-host localhost --ledger-port 6865 --ledger-user admin"
Start-Sleep 10

Write-Output "Starting Trigger 3"
Start-Process -NoNewWindow daml -ArgumentList "trigger --dar triggers/triggers.dar --trigger-name AcceptAssetInviteTrigger:acceptAssetInviteTrigger --ledger-host localhost --ledger-port 6865 --ledger-user admin"
Start-Sleep 10

Write-Output "Starting Trigger 4"
Start-Process -NoNewWindow daml -ArgumentList "trigger --dar triggers/triggers.dar --trigger-name AcceptSwapTrigger:acceptSwapTrigger --ledger-host localhost --ledger-port 6865 --ledger-user admin"
Start-Sleep 10
