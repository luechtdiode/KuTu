import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { ClubregEditorPage } from './clubreg-editor.page';
import { AutoCompleteModule } from 'ionic4-auto-complete';

const routes: Routes = [
  {
    path: '',
    component: ClubregEditorPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    AutoCompleteModule,
    RouterModule.forChild(routes)
  ],
  declarations: [ClubregEditorPage]
})
export class ClubregEditorPageModule {}
