import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule, Routes } from '@angular/router';
import { CompetitionListPage } from './competition-list.page';

const routes: Routes = [{ path: '', component: CompetitionListPage }];

@NgModule({
  declarations: [CompetitionListPage],
  imports: [CommonModule, IonicModule, RouterModule.forChild(routes)]
})
export class CompetitionListPageModule {}
