package net.speleomaniac.mapit.sencemeterservice;

/**
 * Created by Sencer Coltu on 25.01.2014.
 */

public interface CallbackInterface {
    public void OnReceive(CallbackType callbackType, String data);
    public void OnConnect();
    public void OnDisconnect();
    public void OnStart();
    public void OnStop();
}


