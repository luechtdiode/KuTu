import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { StationGuardService } from './services/station-guard.service';
import { VereinsRegistrationGuardService } from './services/vereins-registration-guard.service';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    loadChildren: './home/home.module#HomePageModule'
  },
  { path: 'station', canActivate: [StationGuardService], loadChildren: './station/station.module#StationPageModule' },
  {
    path: 'wertung-editor/:itemId',
    canActivate: [StationGuardService],
    loadChildren: './wertung-editor/wertung-editor.module#WertungEditorPageModule'
  },
  { path: 'last-results', loadChildren: './last-results/last-results.module#LastResultsPageModule' },
  { path: 'top-results', loadChildren: './last-top-results/last-top-results.module#LastTopResultsPageModule' },
  { path: 'search-athlet', loadChildren: './search-athlet/search-athlet.module#SearchAthletPageModule' },
  { path: 'search-athlet/:wkId', loadChildren: './search-athlet/search-athlet.module#SearchAthletPageModule' },
  { path: 'athlet-view/:wkId/:athletId', loadChildren: './athlet-view/athlet-view.module#AthletViewPageModule' },
  { path: 'registration', loadChildren: './registration/registration.module#RegistrationPageModule' },
  { path: 'registration/:wkId', loadChildren: './registration/registration.module#RegistrationPageModule' },
  { path: 'registration/:wkId/:regId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: './registration/clubreg-editor/clubreg-editor.module#ClubregEditorPageModule' },
  { path: 'reg-athletlist/:wkId/:regId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: './registration/reg-athletlist/reg-athletlist.module#RegAthletlistPageModule' },
  { path: 'reg-athletlist/:wkId/:regId/:athletId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: './registration/reg-athlet-editor/reg-athlet-editor.module#RegAthletEditorPageModule' }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
