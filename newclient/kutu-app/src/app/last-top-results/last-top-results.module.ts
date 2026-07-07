import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { LastTopResultsPage } from './last-top-results.page';
import { ComponentsModule } from '../component/component.module';

const routes: Routes = [
  {
    path: '',
    component: LastTopResultsPage
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
  declarations: [LastTopResultsPage]
})
export class LastTopResultsPageModule {}
