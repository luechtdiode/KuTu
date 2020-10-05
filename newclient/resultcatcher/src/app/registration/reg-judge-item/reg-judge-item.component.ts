import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { JudgeRegistration, JudgeRegistrationProgramItem } from 'src/app/backend-types';

@Component({
  selector: 'app-reg-judge-item',
  templateUrl: './reg-judge-item.component.html',
  styleUrls: ['./reg-judge-item.component.scss'],
})
export class RegJudgeItemComponent implements OnInit {

  
  @Input()
  judgeregistration: JudgeRegistration;

  @Input()
  status: string;

  @Input()
  selectedDisciplinlist: JudgeRegistrationProgramItem[];

  @Output()
  selected = new EventEmitter<JudgeRegistration>();

  ngOnInit() {}

  getProgramList() {
    var pgmList: JudgeRegistrationProgramItem[] = [];
    if (this.selectedDisciplinlist === undefined) {
      this.selectedDisciplinlist = [];
    }
    this.selectedDisciplinlist.forEach(element => {
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
}
