[![Release](https://jitpack.io/v/open-voip-alliance/Android-VoIP-Lib.svg)](https://jitpack.io/#open-voip-alliance/Android-VoIP-Lib)

# AndroidVoIPLib

This is library is an opinionated VoIP wrapper for Android applications. It currently uses Linphone as the underlying SIP library.

## Installation

Install using [Jitpack.io](https://jitpack.io/#open-voip-alliance/Android-VoIP-Lib).

## Registration

Step 1: Create a Config object . This object contains all the possible configuration options, however the auth parameter is the only one that is required, the rest will use sensible defaults.

```
val config = Config(
   auth = Auth("username", "password", "domain", "port"),
   callListener = callListener
)
```

Step 2: Get an instance of the VoIPLib and initialise it with the config

```
val voipLib = VoIPLib.getInstance(CONTEXT).initialise(config)
```

Step 3: Call `register` to register with the authentication information provided in the config

```
voipLib.register {  registrationState ->
       if (registrationState == RegistrationState.REGISTERED) {
           // registration was successful
       }
}
```

To `unregister` use:

```
voipLib.unregister()
```

## Calling

To receive events you must provide a callListener that implements the CallListener interface, this provides a handful of simple methods that will allow your application to update based on the current state of the call. There is further documentation within the CallListener class as to what each callback method does.

Once created this object should be provided in the config provided.

The CallListener listens for the following events:

 - incomingCallReceived
 - outgoingCallCreated
 - callConnected
 - callEnded
 - callUpdated
 - error

All of these methods will provide a Call object, listening to these methods is the only way to obtain a call object so actions can be performed on it.

### Outgoing call

Once registered you can make a call as follows:

```
voipLib.callTo('0612345678')
```

If setup successfully this will be followed shortly by a call to the outgoingCallCreated method which will provide a Call object.

### Incoming call

When successfully registered, the library will be listening for incoming calls. If one is received the incomingCallReceived callback method will be triggered.

Answering or declining an incoming call are considered actions, you can perform an action on a call as follows:

    voipLib.actions(call).accept()

or

    voipLib.actions(call).decline()

There are many more actions available for calls, please look inspect the Actions class to see what more can be done to active calls.

 ## Basic Example

The library contains an example app that shows more clearly how to use the library but the following is a very basic example for making an outgoing call.



    val config = Config(
      auth = Auth("my username", "my password", "sip.com", 5061),
      encryption = true,
      stun = "mystunserver.com",
      codecs = arrayOf(Codec.OPUS)
    )

    val voipLib = VoIPLib.getInstance(context).initialise(config)

    voipLib.register {
	    if (it == RegistrationState.REGISTERED) {
		   voipLib.callTo("0123456789")
	    }
    }

 ## Notes

### Service

This library does not include an Android Service, if you wish for calls to continue when your application does not have an Activity in the foreground, you will need to implement a [Foreground Service](https://developer.android.com/guide/components/foreground-services).

### Background Incoming Calls

Incoming calls on mobile devices typically use push notifications to trigger a registration, this is the our recommendation but implementing it is out of scope for this library.

### Recommended Usage

Although other use-cases are supported, we suggest performing a full initialisation before each call, and completely destroying the library after the call has completed.

 ## Pull Requests

This library is designed to simplify using VoIP in an Android application and as such must be somewhat opinionated. This means the library will not support every possible situation.

Any pull requests should keep this in-mind as they will not be approved if they add a significant amount of complexity.

## Other
If you have any question please let us know via `opensource@wearespindle.com` or by opening an issue.
