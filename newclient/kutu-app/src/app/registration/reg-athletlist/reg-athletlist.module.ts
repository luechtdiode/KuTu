import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { RegAthletlistPage } from './reg-athletlist.page';
import { RegAthletItemComponent } from '../reg-athlet-item/reg-athlet-item.component';

const routes: Routes = [
  {
    path: '',
    component: RegAthletlistPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    RouterModule.forChild(routes)
  ],
  exports: [RegAthletlistPage, RegAthletItemComponent],
  declarations: [RegAthletlistPage, RegAthletItemComponent]
})
export class RegAthletlistPageModule {}
