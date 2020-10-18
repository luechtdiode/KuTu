import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { RegJudgeEditorPageRoutingModule } from './reg-judge-editor-routing.module';

import { RegJudgeEditorPage } from './reg-judge-editor.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    RegJudgeEditorPageRoutingModule
  ],
  declarations: [RegJudgeEditorPage]
})
export class RegJudgeEditorPageModule {}
