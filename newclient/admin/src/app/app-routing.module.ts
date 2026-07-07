import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  { path: '', redirectTo: 'competitions', pathMatch: 'full' },
  {
    path: 'competitions',
    loadChildren: () => import('./competition-list/competition-list.module').then(m => m.CompetitionListPageModule)
  },
  {
    path: 'competitions/create',
    loadChildren: () => import('./create-competition/create-competition.module').then(m => m.CreateCompetitionPageModule)
  },
  {
    path: 'security',
    loadChildren: () => import('./security/security.module').then(m => m.SecurityPageModule)
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
