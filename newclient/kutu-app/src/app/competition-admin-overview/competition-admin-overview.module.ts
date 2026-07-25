import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { CompetitionAdminOverviewPage } from './competition-admin-overview.page';

const routes: Routes = [{ path: '', component: CompetitionAdminOverviewPage }];

@NgModule({
  declarations: [CompetitionAdminOverviewPage],
  imports: [CommonModule, IonicModule, RouterModule.forChild(routes)]
})
export class CompetitionAdminOverviewPageModule {}
