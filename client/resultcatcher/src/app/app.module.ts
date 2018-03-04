import { BrowserModule } from '@angular/platform-browser';
import { ErrorHandler, NgModule } from '@angular/core';
import { IonicApp, IonicErrorHandler, IonicModule } from 'ionic-angular';

import { MyApp } from './app.component';
import { HomePage } from '../pages/home/home';
import { RiegeListPage } from '../pages/list/riege-list';

import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { TokenInterceptor } from './token-interceptor';
import { BackendService } from './backend.service';
import { WertungEditorPage } from '../pages/wertung-editor/wertung-editor';
import { SettingsPage } from '../pages/settings/settings';
import { StationPage } from '../pages/station/station';

@NgModule({
  declarations: [
    MyApp,
    HomePage,
    StationPage,
    SettingsPage,
    RiegeListPage,
    WertungEditorPage
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    IonicModule.forRoot(MyApp),
  ],
  bootstrap: [IonicApp],
  entryComponents: [
    MyApp,
    HomePage,
    StationPage,
    SettingsPage,
    RiegeListPage,
    WertungEditorPage
  ],
  providers: [
    StatusBar,
    SplashScreen,
    BackendService,
    TokenInterceptor,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true
    },
    {provide: ErrorHandler, useClass: IonicErrorHandler}
  ]
})
export class AppModule {}
