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
    path: 'competitions',
    loadChildren: () => import('./competitions/competitions.module').then( m => m.CompetitionsPageModule)
  },
  {
    path: 'home',
    loadChildren: () => import('./home/home.module').then( m => m.HomePageModule)
  },
  { path: 'station', canActivate: [StationGuardService], loadChildren: () => import('./station/station.module').then( m => m.StationPageModule) },
  {
    path: 'wertung-editor/:itemId',
    canActivate: [StationGuardService],
    loadChildren: () => import('./wertung-editor/wertung-editor.module').then( m => m.WertungEditorPageModule)
  },
  { path: 'last-results', loadChildren: () => import('./last-results/last-results.module').then( m => m.LastResultsPageModule) },
  { path: 'top-results', loadChildren: () => import('./last-top-results/last-top-results.module').then( m => m.LastTopResultsPageModule) },
  { path: 'search-athlet', loadChildren: () => import('./search-athlet/search-athlet.module').then( m => m.SearchAthletPageModule) },
  { path: 'search-athlet/:wkId', loadChildren: () => import('./search-athlet/search-athlet.module').then( m => m.SearchAthletPageModule) },
  { path: 'athlet-view/:wkId/:athletId', loadChildren: () => import('./athlet-view/athlet-view.module').then( m => m.AthletViewPageModule) },
  { path: 'registration', loadChildren: () => import('./registration/registration.module').then( m => m.RegistrationPageModule) },
  { path: 'registration/:wkId', loadChildren: () => import('./registration/registration.module').then( m => m.RegistrationPageModule) },
  { path: 'registration/:wkId/:regId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: () => import('./registration/clubreg-editor/clubreg-editor.module').then( m => m.ClubregEditorPageModule) },
  { path: 'reg-athletlist/:wkId/:regId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: () => import('./registration/reg-athletlist/reg-athletlist.module').then( m => m.RegAthletlistPageModule) },
  { path: 'reg-athletlist/:wkId/:regId/:athletId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: () => import('./registration/reg-athlet-editor/reg-athlet-editor.module').then( m => m.RegAthletEditorPageModule) },
  {
    path: 'reg-judgelist/:wkId/:regId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: () => import('./registration/reg-judgelist/reg-judgelist.module').then( m => m.RegJudgelistPageModule) },
  {
    path: 'reg-judgelist/:wkId/:regId/:judgeId',
    canActivate: [VereinsRegistrationGuardService],
    loadChildren: () => import('./registration/reg-judge-editor/reg-judge-editor.module').then( m => m.RegJudgeEditorPageModule)
  },
  {
    path: 'admin',
    redirectTo: 'admin/competitions',
    pathMatch: 'full'
  },
  {
    path: 'admin/competitions',
    loadChildren: () => import('./competition-list/competition-list.module').then(m => m.CompetitionListPageModule)
  },
  {
    path: 'admin/competitions/create',
    loadChildren: () => import('./create-competition/create-competition.module').then(m => m.CreateCompetitionPageModule)
  },
  {
    path: 'admin/security',
    loadChildren: () => import('./security/security.module').then(m => m.SecurityPageModule)
  },
  {
    path: 'admin/riege-einteilung/:uuid',
    loadChildren: () => import('./riege-einteilung/riege-einteilung.module').then(m => m.RiegeEinteilungPageModule)
  },
  {
    path: 'admin/registrations/:uuid',
    loadChildren: () => import('./admin-registrations/admin-registrations.module').then(m => m.AdminRegistrationsPageModule)
  }

];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules/*, relativeLinkResolution: 'legacy'*/ })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
