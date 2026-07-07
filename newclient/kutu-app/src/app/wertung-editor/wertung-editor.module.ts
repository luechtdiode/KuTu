import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { WertungEditorPage } from './wertung-editor.page';
import { ComponentsModule } from '../component/component.module';

const routes: Routes = [
  {
    path: '',
    component: WertungEditorPage
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
  providers: [],
  declarations: [WertungEditorPage]
})
export class WertungEditorPageModule {}
