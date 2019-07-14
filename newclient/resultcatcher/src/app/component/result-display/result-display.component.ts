import { Component, OnInit, Input } from '@angular/core';
import { WertungContainer } from 'src/app/backend-types';

@Component({
  selector: 'result-display',
  templateUrl: './result-display.component.html',
  styleUrls: ['./result-display.component.scss'],
})
export class ResultDisplayComponent implements OnInit {
  @Input()
  item: WertungContainer;

  @Input()
  title: string;

  constructor() { }

  ngOnInit() {}

}
