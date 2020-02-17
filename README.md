Lndroid.Framework - build Lightning wallets on Android
======================================================

Lndroid.Framework makes it easy to add [Lightning](https://lightning.network/) wallet into your Android app. It also allows you to expose an IPC API for other apps to interact with your wallet.

***My belief is that the future of Lightning Network is in mobile.***

More and more mobile apps will be integrating a Lightning wallet to make use of this incredible technilogy. More and more wallets will be created, exploring the huge tech space that Lightning enables. Lndroid.Framework is a high-level wrapper around [Lightning Network Daemon (lnd)](https://github.com/lightningnetwork/lnd), with an API that fits nicely with Android components like Jetpack LiveData or Paging. With Lndroid, building a wallet is just a matter of adding some UI. 

But then not every app needs a whole Lightning wallet (which is quite heavy). Some apps would prefer to access an existing wallet with an IPC API. That's why Lndroid.Framework allows wallets to expose select API methods over IPC, and provides access control management and all that. This effectively turns wallets into mobile payment platforms fully owned by end user, and should finally make "never seen before" apps like pay-per view web-browsing or pay-per-second youtube practical.

Plus, Lndroid.Framework makes use of cool mobile technologies, like Keystore and Notifications, and handles various mobile restrictions like intermittent network connectivity. For instance, whenever a payment fails, it might spawn a Service to retry the payment. It might also schedule a background service to start the wallet every now and then to accept payments. This finally makes it practical to send payments between mobile wallets.

To see a wallet based on Lndroid.Framework, check [Lndroid.Wallet](https://github.com/lndroid/lndroid-wallet/). To see an app integrated with the framework-based wallet, check [Lndroid.Messenger](https://github.com/lndroid/lndroid-messenger/). The client-side library for the apps is [Lndroid.Client](https://github.com/lndroid/lndroid-client/) (though it's sources are just an excerpt from the framework).

To see sample app+wallet in action, check [this video](https://www.youtube.com/watch?v=bF-1QxFTvHU), with description available [here](https://github.com/lndroid/lndroid-wallet/#here-is-what-you-see-on-the-lndroid-demo-video).

# Roadmap
- [x] Multi-user access, with 'app' user role given to Apps with IPC access
- [x] Unified API access mechanism based on message passing, allowing to implement various patterns like RPC, streaming, etc
- [x] All API methods are plugins, which can be added/replaced/modified by wallets
- [x] Every API method defines it's own set of access conditions/permissions, and might trigger authorization request to be confirmed by human User
- [x] Pluggable database layer to store framework data, and to cache lnd data to serve apps faster, default - Android Room
- [x] Pluggable codec to serialize IPC messages, default - GSON
- [x] Pluggable key storage, which by default stores lnd wallet password encrypted by key from the keystore, allowing lnd to be started in the background when Apps connect to the API, or when background jobs are running
- [x] All API methods are idempotent: client might generate a random 'transaction id', save it, then start an API call with it, and if it's interrupted due to client/wallet failure or termination by OS, the same call might be resumed using the transaction id
- [x] Connected apps are identified by a random pubkey, framework generates per-user (thus per-app) keypairs and passed it's pubkeys to apps on connect
- [x] Failed payments are retried using Foreground Service up to max-tries and payment expiry
- [x] Scheduled job to run lnd in background every X minutes for several minutes to accept payments
- [x] Scheduled job to run lnd in background every X hours for ~1 hour to come in sync with network and node graph
- [ ] Messages over IPC are signed using app/wallet private key, signatures verified by receiver using sender pubkey
- [x] Authentication for internal users (role != app), at least allow passwords and also device security facilities like 'device unlocked', bio/face id, etc 
- [ ] Authentication UI for internal users, default UI for password plus call BioPrompt when necessary (if keys are unavailable)
- [ ] Validation of every input params by every API method
- [ ] Unit tests! Haha, we need some :)
- [ ] Channel backups stored to local dir
- [ ] Lnd and framework logs recorded and exposed over API 
- [ ] Framework and Lnd API input/output messages saved and exposed over API 
- [ ] Notifications: add API methods to start subscriptions, so that if wallet detects that client is not connected, sends Android Broadcast message that wakes up the client and allows it to consume the data requested by the call
- [ ] Method/data versioning? Prepare for the future
- [ ] First release

- [ ] Database records with long-term effects like new users or privileges are signed by author's private key, signatures verified on every use of the record, if keys are invalidated records must be re-signed. 
- [ ] Per-user keys are stored in AndroidKeystore using proper policies, validity of keys verified by signature of the parent user up to root, root user's key signed by keys derived from wallet password, thus if any of keys are invalidated due to policies or wallet password changes, the signed permissions/users in db must be re-signed which must require User interaction with the phone.
- [ ] Macaroons: explore if macaroons can be given in exchange for permissions, if this could help apps provide more granular security to different macaroons using KeyStore policies
- [ ] App spending (and other) limits: granular permissions are flexible but hard to understand for Users, simple per-app limits could be enough 
- [ ] Permissions/privileges: explore the space of various options depending on apps needs
- [ ] Lightning protocol extensions (Whatsat, etc): explore if we need specialized API methods and permissions, like 'default protocol app'
- [ ] Encrypted channel backup uploaded to common cloud storage services
- [ ] Framework database is encrypted using keys derived from wallet password

# Contributions wanted

- [ ] LNURL support
- [ ] WebLN support
- [ ] NFC
- [ ] WatchTowers

Author is not an expert in lnd or crypto (or Android, for the matter), your advice and contributions will really help to bring Lndroid forward.

# TODO

...This readme is just an intro, expand it to properly cover what Lndroid does, and how to use it...

# Dependencies

1. Android Room
2. Protobuf
3. AutoValue
4. Guava
5. Gson
6. Lndroid.Daemon

# Important

The whole Lndroid project is at the very early stages of development. No guarantees are made about API stability, or the like. Do not use Lndroid code on Bitcoin mainnet, as you're very likely to lose your funds.

# License

MIT

# Author

Artur Brugeman, brugeman.artur@gmail.com