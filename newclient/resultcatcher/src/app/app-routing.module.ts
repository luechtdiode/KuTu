import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { StationGuardService } from './services/station-guard.service';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    loadChildren: './home/home.module#HomePageModule'
    // loadChildren: './station/station.module#StationPageModule'
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
  { path: 'athlet-view/:wkId/:athletId', loadChildren: './athlet-view/athlet-view.module#AthletViewPageModule' }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule {}
