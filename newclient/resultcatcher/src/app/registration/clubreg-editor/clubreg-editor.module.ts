import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';

import { ClubregEditorPage } from './clubreg-editor.page';
import { ComponentsModule } from 'src/app/component/component.module';

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
    RouterModule.forChild(routes),
    ComponentsModule
  ],
  declarations: [ClubregEditorPage]
})
export class ClubregEditorPageModule {}
