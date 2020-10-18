import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { RegJudgeEditorPage } from './reg-judge-editor.page';

const routes: Routes = [
  {
    path: '',
    component: RegJudgeEditorPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RegJudgeEditorPageRoutingModule {}
