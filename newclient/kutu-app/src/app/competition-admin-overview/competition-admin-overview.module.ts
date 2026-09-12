import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { CompetitionAdminOverviewPage } from './competition-admin-overview.page';
import { AdminAccessLinkModalComponent } from './admin-access-link-modal.component';
import {StandardLinkModalComponent} from "./standard-link-modal.component";

const routes: Routes = [{ path: '', component: CompetitionAdminOverviewPage }];

@NgModule({
  declarations: [CompetitionAdminOverviewPage, AdminAccessLinkModalComponent, StandardLinkModalComponent],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class CompetitionAdminOverviewPageModule {}
