import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminRankingsPage } from './admin-rankings.page';
import { ComponentsModule } from '../component/component.module';

const routes: Routes = [{ path: '', component: AdminRankingsPage }];

@NgModule({
  declarations: [AdminRankingsPage],
  imports: [CommonModule, IonicModule, FormsModule, RouterModule.forChild(routes), ComponentsModule]
})
export class AdminRankingsPageModule {}
