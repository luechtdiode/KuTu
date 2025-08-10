import { isNgTemplate } from '@angular/compiler';
import { Component, OnInit, input, output } from '@angular/core';
import { JudgeRegistration, JudgeRegistrationProgramItem } from 'src/app/backend-types';

@Component({
    selector: 'app-reg-judge-item',
    templateUrl: './reg-judge-item.component.html',
    styleUrls: ['./reg-judge-item.component.scss'],
    standalone: false
})
export class RegJudgeItemComponent implements OnInit {

  
  readonly judgeregistration = input.required<JudgeRegistration>();

  readonly selectedDisciplinlist = input<JudgeRegistrationProgramItem[]>([]);

  readonly selected = output<JudgeRegistration>();

  ngOnInit() {}

  getProgramList() {
    let pgmList: JudgeRegistrationProgramItem[] = [];
    const selectedDisciplinlist = this.selectedDisciplinlist();
    selectedDisciplinlist.forEach(element => {
      if(pgmList.find(pgm => pgm.program == element.program) === undefined) {
        pgmList.push(element);
      }
    });
    return pgmList;
  }

  getProgrammDisciplinText(item: JudgeRegistrationProgramItem) {
    return item.program + '/' + item.disziplin;
  }

  getProgrammText(item: JudgeRegistrationProgramItem) {
    return item.program;
  }

  getCommentLines() {
    return this.judgeregistration().comment.split('\n');
  }
}
