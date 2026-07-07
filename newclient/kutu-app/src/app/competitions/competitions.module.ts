import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { CompetitionsPage } from './competitions.page';
import { RouterModule, Routes } from '@angular/router';
import { ComponentsModule } from '../component/component.module';

const routes: Routes = [
  {
    path: '',
    component: CompetitionsPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    RouterModule.forChild(routes),
    ComponentsModule
  ],
  declarations: [CompetitionsPage]
})
export class CompetitionsPageModule {}
