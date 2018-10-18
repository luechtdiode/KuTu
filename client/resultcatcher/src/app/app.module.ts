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
import { StationPage } from '../pages/station/station';
import { LastResultDetailPage } from '../pages/last-result-detail/last-result-detail';
import { LastResultsPage } from '../pages/last-results/last-results';
import { LastTopResultsPage } from '../pages/last-top-results/last-top-results';
import { ResultDisplayComponent } from '../components/result-display/result-display';

@NgModule({
  declarations: [
    MyApp,
    HomePage,
    StationPage,
    RiegeListPage,
    WertungEditorPage,
    LastResultsPage,
    LastResultDetailPage,
    LastTopResultsPage,
    ResultDisplayComponent
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
    RiegeListPage,
    WertungEditorPage,
    LastResultsPage,
    LastResultDetailPage,
    LastTopResultsPage
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
