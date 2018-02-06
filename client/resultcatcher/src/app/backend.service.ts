import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { backendUrl } from './utils';
import { TokenInterceptor } from './token-interceptor';
import { toBase64String } from '@angular/compiler/src/output/source_map';
import { WertungContainer, Geraet, Wettkampf, Wertung } from './backend-types';

declare var location: any;

@Injectable()
export class BackendService {
  loggedIn = false;

  competitions: Wettkampf[];
  durchgaenge: string[];
  geraete: Geraet[];
  steps: string[];
  wertungen: WertungContainer[];

  constructor(public http: HttpClient) {
    this.loggedIn = !!localStorage.getItem('auth_token');
  }

  login(username, password) {
    const headers = new HttpHeaders();
    this.http.options(backendUrl + 'api/login', { 
      observe: 'response', 
      headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password)),
      withCredentials: true,
      responseType: 'text'
    }).subscribe((data) => {
      console.log(data);
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn = !!localStorage.getItem('auth_token');
    }, (err) => {
      console.log(err);
    });
  }

  logout() {
    localStorage.removeItem('auth_token');
    this.loggedIn = !!localStorage.getItem('auth_token');
  }

  getCompetitions() {
    this.http.get<Wettkampf[]>(backendUrl + 'api/competition').subscribe((data) => {
      this.competitions = data;
    });
  }

  getDurchgaenge(competitionId: number) {
    this.durchgaenge = undefined;
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<string[]>(backendUrl + 'api/competition/' + competitionId).subscribe((data) => {
      this.durchgaenge = data;
    });
  }

  getGeraete(competitionId, durchgang) {
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<Geraet[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang).subscribe((data) => {
      this.geraete = data;
    });
  }

  getSteps(competitionId, durchgang, geraetId) {
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<string[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId).subscribe((data) => {
      this.steps = data;
    });
  }

  getWertungen(competitionId, durchgang, geraetId, step) {
    this.wertungen = undefined;
    this.http.get<WertungContainer[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId + '/' + step).subscribe((data) => {
      this.wertungen = data;
    });
  }

  updateWertung(durchgang: string, step: number, geraetId: number, wertung: Wertung) {
    const competitionId = wertung.wettkampfId;
    this.http.put<WertungContainer[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId + '/' + step, wertung).subscribe((data) => {
      this.wertungen = data;
    });    
  }
}