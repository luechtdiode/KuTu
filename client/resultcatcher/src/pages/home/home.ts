import { Component } from '@angular/core';
import { NavController } from 'ionic-angular';
import { BackendService } from '../../app/backend.service';
import { Wettkampf, Geraet } from '../../app/backend-types';
import { StationPage } from '../station/station';
import { encodeURIComponent2 } from '../../app/websocket.service';


@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {

  durchgangstate: string;
  durchgangopen = false;
  
  constructor(public navCtrl: NavController, public backendService: BackendService) {
    this.backendService.getCompetitions();
    this.backendService.durchgangStarted.map(dgl => 
      dgl.filter(dg => encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.backendService.durchgang) && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    ).subscribe(dg => {
      this.durchgangstate = dg ? 'gestartet' : 'gesperrt';
      this.durchgangopen = dg;
    });
  }

  set competition(competitionId: string) {
    if(!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
    }
  }
  get competition(): string {
    return this.backendService.competition || "";
  }

  set durchgang(d: string) {
    if(!this.stationFreezed) {
      this.backendService.getGeraete(this.competition, d);
    }
  }
  get durchgang(): string {
    return this.backendService.durchgang || "";
  }

  set geraet(geraetId: number) {
    if(!this.stationFreezed) {
      this.backendService.getSteps(this.competition, this.durchgang, geraetId);
    }
  }
  get geraet(): number {
    return this.backendService.geraet || -1;
  }

  set step(s) {
    this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
  }
  get step() {
    return this.backendService.step || -1;
  }

  get stationFreezed(): Boolean {
    return this.backendService.stationFreezed;
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName(): string {
    if (!this.backendService.competitions) return '';
    let candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join("-"));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetName(): string {
    if (!this.backendService.geraete) return '';
    let candidate = this.backendService.geraete
      .filter(c => c.id === this.backendService.geraet)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }
  getDurchgaenge(): string[] {
    return this.backendService.durchgaenge;
  }

  getGeraete(): Geraet[] {
    return this.backendService.geraete;
  }
  
  getSteps(): number[] {
    if (! this.backendService.steps && this.backendService.geraet) {
      this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
    }
    return this.backendService.steps;
  }  

  navToStation() {
    this.navCtrl.swipeBackEnabled = true;
    this.navCtrl.push(StationPage);
  }
  logout() {
    this.backendService.logout();
  }
  isLoggedIn() {
    return this.backendService.loggedIn;
  }
  finish() {
    this.backendService.finishStation(this.competition, this.durchgang, this.step, this.geraet);
  }
}
