"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[3050],{3050:(L,p,a)=>{a.r(p),a.d(p,{StationPageModule:()=>R});var l=a(6895),f=a(4719),v=a(229),o=a(9928),u=a(5861),h=a(8204),_=a(4850),e=a(5675),m=a(600);function b(i,s){if(1&i&&(e.TgZ(0,"ion-note",7),e._uU(1),e.ALo(2,"number"),e.qZA()),2&i){const t=e.oxw().$implicit;e.xp6(1),e.hij(" ",e.xi3(2,1,t.wertung.endnote,"2.3-3")," ")}}function S(i,s){1&i&&(e.TgZ(0,"ion-note",8),e._uU(1,"Kein Resultat"),e.qZA())}function k(i,s){if(1&i){const t=e.EpF();e.TgZ(0,"ion-item",2),e.NdJ("click",function(r){const c=e.CHM(t).$implicit,d=e.oxw(2);return e.KtG(d.itemTapped(r,c))}),e.TgZ(1,"ion-avatar",3),e._UZ(2,"img",4),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e.TgZ(5,"small"),e._uU(6),e.qZA()(),e.YNc(7,b,3,4,"ion-note",5),e.YNc(8,S,2,0,"ion-note",6),e.qZA()}if(2&i){const t=s.$implicit;e.xp6(4),e.hij("",t.vorname+" "+t.name," "),e.xp6(2),e.Oqu(" ("+t.wertung.riege+")"),e.xp6(1),e.Q6J("ngIf",void 0!==t.wertung.endnote),e.xp6(1),e.Q6J("ngIf",void 0===t.wertung.endnote)}}function C(i,s){if(1&i&&(e.TgZ(0,"ion-list"),e.YNc(1,k,9,4,"ion-item",1),e.qZA()),2&i){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.items)}}let Z=(()=>{class i{constructor(t,n){this.navCtrl=t,this.backendService=n}itemTapped(t,n){this.navCtrl.navigateForward(this.backendService.loggedIn?"wertung-editor/"+n.id:`athlet-view/${this.backendService.competition}/${n.id}`)}}return i.\u0275fac=function(t){return new(t||i)(e.Y36(o.SH),e.Y36(m.v))},i.\u0275cmp=e.Xpm({type:i,selectors:[["riege-list"]],inputs:{items:"items"},decls:1,vars:1,consts:[[4,"ngIf"],[3,"click",4,"ngFor","ngForOf"],[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end","class","result-value",4,"ngIf"],["slot","end","class","result-no-value",4,"ngIf"],["slot","end",1,"result-value"],["slot","end",1,"result-no-value"]],template:function(t,n){1&t&&e.YNc(0,C,2,1,"ion-list",0),2&t&&e.Q6J("ngIf",n.items&&n.items.length>0)},dependencies:[l.sg,l.O5,o.BJ,o.Ie,o.Q$,o.q_,o.uN,l.JJ],styles:[".result-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-success)}.result-no-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-warning)}"]}),i})();function x(i,s){1&i&&(e.TgZ(0,"div")(1,"ion-title"),e._uU(2,"Resultate"),e.qZA()())}function T(i,s){if(1&i&&(e.TgZ(0,"div")(1,"ion-title"),e._uU(2,"Resultaterfassung"),e.TgZ(3,"div",15),e._uU(4),e.qZA()()()),2&i){const t=e.oxw();e.xp6(3),e.Q6J("ngClass",t.durchgangstateClass),e.xp6(1),e.Oqu(t.durchgangstate())}}function A(i,s){if(1&i&&(e._UZ(0,"riege-list",16),e.ALo(1,"async")),2&i){const t=e.oxw();e.Q6J("items",e.lcZ(1,1,t.getWertungen()))}}function w(i,s){if(1&i&&(e.TgZ(0,"ion-note",17),e._uU(1),e.qZA()),2&i){const t=e.oxw();e.xp6(1),e.AsE("(",t.getSteps()[0]," bis ",t.getSteps()[t.getSteps().length-1],")")}}function U(i,s){1&i&&e._UZ(0,"ion-icon",18)}function J(i,s){if(1&i){const t=e.EpF();e.TgZ(0,"ion-button",19),e.NdJ("click",function(){e.CHM(t);const r=e.oxw();return e.KtG(r.finish())}),e._UZ(1,"ion-icon",20),e._uU(2," Eingaben abschliessen "),e.qZA()}if(2&i){const t=e.oxw();e.Q6J("disabled",!(t.geraet&&t.getSteps()))}}let N=(()=>{class i{constructor(t,n,r,g){this.navCtrl=t,this.backendService=n,this.alertCtrl=r,this.zone=g,this.durchgangopen=!1,this.geraete=[],this.backendService.durchgangStarted.pipe((0,_.U)(c=>c.filter(d=>(0,h.gT)(d.durchgang)===(0,h.gT)(this.backendService.durchgang)&&d.wettkampfUUID===this.backendService.competition).length>0)).subscribe(c=>{this.durchgangopen=c})}ionViewWillEnter(){this.geraete.length>0&&!this.backendService.captionmode&&this.backendService.activateCaptionMode()}ngOnInit(){this.backendService.geraeteSubject.subscribe(t=>{this.backendService.captionmode&&(this.geraete=t)})}durchgangstate(){return this.backendService.isWebsocketConnected()?this.durchgangopen?"gestartet":"gesperrt":"offline"}get durchgangstateClass(){return{open:this.backendService.isWebsocketConnected()&&this.durchgangopen,closed:this.backendService.isWebsocketConnected()&&!this.durchgangopen,offline:!this.backendService.isWebsocketConnected()}}set competition(t){this.stationFreezed||this.backendService.getDurchgaenge(t)}get competition(){return this.backendService.competition}set durchgang(t){this.stationFreezed||this.backendService.getGeraete(this.competition,t)}get durchgang(){return this.backendService.durchgang}set geraet(t){this.stationFreezed||this.backendService.getSteps(this.competition,this.durchgang,t)}get geraet(){return this.backendService.geraet}set step(t){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,t)}get step(){return this.backendService.step}get stationFreezed(){return this.backendService.stationFreezed}isLoggedIn(){return this.backendService.loggedIn}get nextStepCaption(){return this.isLoggedIn()?"N\xe4chste Riege":"N\xe4chstes Ger\xe4t"}get prevStepCaption(){return this.isLoggedIn()?"Vorherige Riege":"Vorheriges Ger\xe4t"}itemTapped(t){t.getOpenAmount().then(n=>{n>10||n<-10?t.close():t.open("end")})}nextStep(t){this.backendService.nextGeraet().subscribe(n=>{this.step=n,t.close()})}prevStep(t){this.backendService.prevGeraet().subscribe(n=>{this.step=n,t.close()})}get station(){return this.isLoggedIn()?this.step+". Riege "+this.geraetName()+", "+this.durchgang:this.step+". Ger\xe4t "+this.geraetName()+", "+this.durchgang}showCompleteAlert(){var t=this;return(0,u.Z)(function*(){const n=t.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+t.station+'" k\xf6nnen die erfassten Wertungen nur noch im Wettkampf-B\xfcro korrigiert werden.',buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{const r=n.then(g=>g.dismiss());return t.zone.run(()=>{t.backendService.finishStation(t.competition,t.durchgang,t.geraet,t.step).subscribe(g=>{0===g.length&&r.then(()=>{t.navCtrl.pop()})})}),!1}}]});n.then(r=>r.present())})()}toastMissingResult(t){var n=this;return(0,u.Z)(function*(){const r=t[0].vorname.toUpperCase()+" "+t[0].name.toUpperCase()+" ("+t[0].verein+")";(yield n.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"In dieser Riege gibt es noch "+t.length+" leere Wertungen! - Bitte pr\xfcfen, ob "+r+" geturnt hat.",buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:r+" hat nicht geturnt",role:"edit",handler:()=>{t.splice(0,1),t.length>0?n.toastMissingResult(t):n.showCompleteAlert()}},{text:"Korrigieren",role:"edit",handler:()=>{n.navCtrl.navigateForward("wertung-editor/"+t[0].id)}}]})).present()})()}finish(){const t=this.backendService.wertungen.filter(n=>void 0===n.wertung.endnote);0!==t.length?this.toastMissingResult(t):this.showCompleteAlert()}getCompetitions(){return this.backendService.competitions}competitionName(){if(!this.backendService.competitions)return"";const t=this.backendService.competitions.filter(n=>n.uuid===this.backendService.competition).map(n=>n.titel+"<br>"+(n.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===t.length?t[0]:""}geraetName(){if(!this.geraete||0===this.geraete.length)return"";const t=this.geraete.filter(n=>n.id===this.backendService.geraet).map(n=>n.name);return 1===t.length?t[0]:""}getDurchgaenge(){return this.backendService.durchgaenge}getGeraete(){return this.geraete}getSteps(){return this.backendService.steps}getWertungen(){return this.backendService.wertungenSubject}}return i.\u0275fac=function(t){return new(t||i)(e.Y36(o.SH),e.Y36(m.v),e.Y36(o.Br),e.Y36(e.R0b))},i.\u0275cmp=e.Xpm({type:i,selectors:[["app-station"]],decls:29,vars:14,consts:[["slot","start"],["defaultHref","/"],[4,"ngIf"],["slot","end",1,"ion-padding-end",3,"innerHtml"],[3,"items",4,"ngIf"],["slidingStepItem",""],[3,"click"],["slot","end",4,"ngIf"],["slot","end","name","arrow-dropright",4,"ngIf"],["side","start"],["color","primary",3,"disabled","click"],["name","arrow-dropleft-circle","ios","md-arrow-dropleft-circle"],["side","end"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["size","large","expand","block","color","success",3,"disabled","click",4,"ngIf"],[1,"stateinfo",3,"ngClass"],[3,"items"],["slot","end"],["slot","end","name","arrow-dropright"],["size","large","expand","block","color","success",3,"disabled","click"],["slot","start","name","git-commit"]],template:function(t,n){if(1&t){const r=e.EpF();e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.YNc(4,x,3,0,"div",2),e.YNc(5,T,5,2,"div",2),e._UZ(6,"ion-note",3),e.qZA()(),e.TgZ(7,"ion-content"),e.YNc(8,A,2,3,"riege-list",4),e.ALo(9,"async"),e.qZA(),e.TgZ(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list")(13,"ion-item-sliding",null,5)(15,"ion-item",6),e.NdJ("click",function(){e.CHM(r);const c=e.MAs(14);return e.KtG(n.itemTapped(c))}),e.TgZ(16,"ion-label"),e._uU(17),e.qZA(),e.YNc(18,w,2,2,"ion-note",7),e.YNc(19,U,1,0,"ion-icon",8),e.qZA(),e.TgZ(20,"ion-item-options",9)(21,"ion-item-option",10),e.NdJ("click",function(){e.CHM(r);const c=e.MAs(14);return e.KtG(n.prevStep(c))}),e._UZ(22,"ion-icon",11),e._uU(23),e.qZA()(),e.TgZ(24,"ion-item-options",12)(25,"ion-item-option",10),e.NdJ("click",function(){e.CHM(r);const c=e.MAs(14);return e.KtG(n.nextStep(c))}),e._UZ(26,"ion-icon",13),e._uU(27),e.qZA()()(),e.YNc(28,J,3,1,"ion-button",14),e.qZA()()()}2&t&&(e.xp6(4),e.Q6J("ngIf",!n.isLoggedIn()),e.xp6(1),e.Q6J("ngIf",n.isLoggedIn()),e.xp6(1),e.Q6J("innerHtml",n.competitionName(),e.oJD),e.xp6(2),e.Q6J("ngIf",n.geraet&&n.durchgang&&n.step&&e.lcZ(9,12,n.getWertungen())),e.xp6(9),e.Oqu(n.station),e.xp6(1),e.Q6J("ngIf",n.getSteps()&&n.getSteps().length>0),e.xp6(1),e.Q6J("ngIf",n.geraet&&n.getSteps()),e.xp6(2),e.Q6J("disabled",!n.geraet||!n.getSteps()),e.xp6(2),e.hij(" ",n.prevStepCaption," "),e.xp6(2),e.Q6J("disabled",!n.geraet||!n.getSteps()),e.xp6(2),e.hij(" ",n.nextStepCaption," "),e.xp6(1),e.Q6J("ngIf",n.isLoggedIn()&&n.durchgangopen))},dependencies:[l.mk,l.O5,o.oU,o.YG,o.Sm,o.W2,o.fr,o.Gu,o.gu,o.Ie,o.u8,o.IK,o.td,o.Q$,o.q_,o.uN,o.sr,o.wd,o.cs,Z,l.Ov],styles:[".stateinfo.closed[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-danger)}.stateinfo.open[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-success)}.stateinfo.offline[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-warning)}"]}),i})();var I=a(5051),P=a(4302);const M=[{path:"",component:N}];let R=(()=>{class i{}return i.\u0275fac=function(t){return new(t||i)},i.\u0275mod=e.oAB({type:i}),i.\u0275inj=e.cJS({providers:[P.N1],imports:[l.ez,f.u5,o.Pc,v.Bz.forChild(M),I.K]}),i})()}}]);