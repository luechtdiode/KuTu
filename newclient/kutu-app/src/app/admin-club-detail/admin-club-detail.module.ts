import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminClubDetailPage } from './admin-club-detail.page';

const routes: Routes = [{ path: '', component: AdminClubDetailPage }];

@NgModule({
  declarations: [AdminClubDetailPage],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminClubDetailPageModule {}
