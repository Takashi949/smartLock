package com.example.smartlock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LockTile extends TileService {
    Tile tile;
    BtCom btCom;
    int isUnlock = 3;

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d("MYAPP", "onSTartListening...  ");
        tile = getQsTile();
        btCom = new BtCom(getApplicationContext(), this);

        btCom.StartAdv();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
        btCom.StopAdv();
        btCom=null;
    }

    @Override
    public void onClick() {
        super.onClick();
        if(btCom.isEspFound && tile.getState() == Tile.STATE_ACTIVE){
            if (isUnlock == BtCom.UnlockState.UNLOCK.ordinal()){
                Log.d("MYAPP", "read unlock tile active");
                btCom.lock();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_lock));
                tile.updateTile();
            }
            else if(isUnlock == BtCom.UnlockState.LOCK.ordinal()){
                Log.d("MYAPP", "read lock tile inactive");
                btCom.ulock();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_open));
                tile.updateTile();
            }
            else if(isUnlock == BtCom.UnlockState.UNKNOWN.ordinal()){
                Log.d("MYAPP", "うんこなう");
                btCom.ulock();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_open));
                tile.updateTile();
            }
        }
    }
    public void readResult(int isUnlock){
        this.isUnlock = isUnlock;
    }
    void espFound(){
        Log.d("MYAPP", "espFound");
        btCom.read();
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    void espLost(){
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }
}