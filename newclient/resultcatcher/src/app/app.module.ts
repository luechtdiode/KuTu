import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouteReuseStrategy } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { BackendService } from './services/backend.service';
import { TokenInterceptor } from './services/token-interceptor';
import { StationGuardService } from './services/station-guard.service';
import { ThemeSwitcherService } from './services/theme-switcher.service';

@NgModule({ declarations: [AppComponent],
    bootstrap: [AppComponent], imports: [BrowserModule,
        IonicModule.forRoot(),
        AppRoutingModule], providers: [
        StationGuardService,
        { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
        BackendService,
        ThemeSwitcherService,
        TokenInterceptor,
        {
            provide: HTTP_INTERCEPTORS,
            useClass: TokenInterceptor,
            multi: true
        },
        provideHttpClient(withInterceptorsFromDi()),
    ] })
export class AppModule {}
