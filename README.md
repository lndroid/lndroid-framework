Lndroid.Framework - build Lightning wallets on Android
======================================================

Lndroid.Framework makes it easy to add [Lightning](https://lightning.network/) wallet into your Android app. It also allows you to expose an IPC API for other apps to interact with your wallet.

***My belief is that the future of Lightning Network is in mobile.***

More and more mobile apps will be integrating a Lightning wallet to make use of this incredible technilogy. More and more wallets will be created, exploring the huge tech space that Lightning enables. Lndroid.Framework is a high-level wrapper around [Lightning Network Daemon (lnd)](https://github.com/lightningnetwork/lnd), with an API that fits nicely with Android components like Jetpack LiveData or Paging. With Lndroid, building a wallet is just a matter of adding some UI. 

But then not every app needs a whole Lightning wallet (which is quite heavy). Some apps would prefer to access an existing wallet with an IPC API. That's why Lndroid.Framework allows wallets to expose select API methods over IPC, and provides access control management and all that. This effectively turns wallets into mobile payment platforms fully owned by end user, and should finally make "never seen before" apps like pay-per view web-browsing or pay-per-second youtube practical.

Plus, Lndroid.Framework makes use of cool mobile technologies, like Keystore and Notifications, and handles various mobile restrictions like intermittent network connectivity. For instance, whenever a payment fails, it might spawn a Service to retry the payment. It might also schedule a background service to start the wallet every now and then to accept payments. This finally makes it practical to send payments between mobile wallets.

To see a wallet based on Lndroid.Framework, check [Lndroid.Wallet](https://github.com/lndroid/lndroid-wallet/). To see an app integrated with a Lightning wallet, check [Lndroid.Messenger](https://github.com/lndroid/lndroid-messenger/).

To see sample app+wallet in action, check this video.

# TODO

...This readme is just an intro, expand it to properly cover what Lndroid does, and how to use it...

# Dependencies

1. Android Room
2. Protobuf
3. Google Auto-Value
4. Guava
5. Gson
6. Lndroid.Daemon

# Important

The whole Lndroid project is at the very early stages of development. No guarantees are made about API stability, or the like. Do not use Lndroid code on Bitcoin mainnet, as you're very likely to lose your funds.

# License

MIT

# Author

Artur Brugeman, brugeman.artur@gmail.com