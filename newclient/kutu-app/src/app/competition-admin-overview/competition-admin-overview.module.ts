import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { CompetitionAdminOverviewPage } from './competition-admin-overview.page';
import { AdminAccessLinkModalComponent } from './admin-access-link-modal.component';
import {StandardLinkModalComponent} from "./standard-link-modal.component";

const routes: Routes = [{ path: '', component: CompetitionAdminOverviewPage }];

@NgModule({
  declarations: [CompetitionAdminOverviewPage, AdminAccessLinkModalComponent, StandardLinkModalComponent],
  imports: [CommonModule, IonicModule, RouterModule.forChild(routes)]
})
export class CompetitionAdminOverviewPageModule {}
