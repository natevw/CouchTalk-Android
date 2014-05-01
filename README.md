Port of [CouchTalk-iOS](https://github.com/natevw/CouchTalk-iOS) to Android. **Work in Progress**

Basic mess of notes/prereqs:

- uses Android Studio, follow CBL tutorial for setup tips

- for now at least use `./node_modules/.bin/grunt dev` in the iOS project to push app

- …i.e. `../CouchTalk-iOS/node_modules/.bin/couchapp push ../CouchTalk-iOS/lib/push.js http://192.168.0.42:59840/couchtalk`

- with AVD emulator prolly need to set up a redirect (each launch…):

    // via http://stackoverflow.com/a/11025806/179583
    // see also <http://developer.android.com/tools/devices/emulator.html#emulatornetworking>
    telnet localhost 5554
    
    redir add tcp:59840:59840
    redir add tcp:8080:8080
    quit
