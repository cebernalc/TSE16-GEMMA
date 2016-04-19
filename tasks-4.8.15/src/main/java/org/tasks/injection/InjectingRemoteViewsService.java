package org.tasks.injection;

import android.widget.RemoteViewsService;

public abstract class InjectingRemoteViewsService extends RemoteViewsService {
    @Override
    public void onCreate() {
        super.onCreate();

        inject(((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ServiceModule()));
    }

    protected abstract void inject(ServiceComponent component);
}
