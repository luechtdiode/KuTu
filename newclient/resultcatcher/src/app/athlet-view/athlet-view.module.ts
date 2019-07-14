import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { AthletViewPage } from './athlet-view.page';
import { ComponentsModule } from '../component/component.module';

const routes: Routes = [
  {
    path: '',
    component: AthletViewPage
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
  declarations: [AthletViewPage]
})
export class AthletViewPageModule {}
