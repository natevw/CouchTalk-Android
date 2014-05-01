Port of [CouchTalk-iOS](https://github.com/natevw/CouchTalk-iOS) to Android. **Work in Progress**

Basic mess of notes/prereqs:

- uses Android Studio, follow CBL tutorial for setup tips

- prolly need to set up a redirect:

    # via http://stackoverflow.com/a/11025806/179583
    # see also <http://developer.android.com/tools/devices/emulator.html#emulatornetworking>
    telnet localhost 5554
    
    redir add tcp:59840:59842
    redir add tcp:59842:59842
    redir add tcp:8080:8080
    quit

- for now at least use `./node_modules/.bin/grunt dev` in the iOS project to push app