"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[379],{379:(z,h,o)=>{o.r(h),o.d(h,{StationPageModule:()=>P});var d=o(177),b=o(4341),S=o(305),r=o(4742),u=o(467),m=o(3537),k=o(6354),p=o(8103),e=o(3953),f=o(2872),v=o(2350);function _(s,c){if(1&s&&(e.j41(0,"ion-note",7),e.EFF(1),e.nI1(2,"number"),e.k0s()),2&s){const t=e.XpG().$implicit;e.R7$(),e.SpI(" ",e.i5U(2,1,t.wertung.endnote,"2.3-3")," ")}}function C(s,c){1&s&&(e.j41(0,"ion-note",8),e.EFF(1,"Kein Resultat"),e.k0s())}function I(s,c){if(1&s){const t=e.RV6();e.j41(0,"ion-item",2),e.bIt("click",function(i){const a=e.eBV(t).$implicit,g=e.XpG(2);return e.Njj(g.itemTapped(i,a))}),e.j41(1,"ion-avatar",3),e.nrm(2,"img",4),e.k0s(),e.j41(3,"ion-label"),e.EFF(4),e.j41(5,"small"),e.EFF(6),e.k0s()(),e.DNE(7,_,3,4,"ion-note",5)(8,C,2,0,"ion-note",6),e.k0s()}if(2&s){const t=c.$implicit;e.R7$(4),e.SpI("",t.vorname+" "+t.name," "),e.R7$(2),e.JRh(" ("+t.wertung.riege+")"),e.R7$(),e.Y8G("ngIf",void 0!==t.wertung.endnote),e.R7$(),e.Y8G("ngIf",void 0===t.wertung.endnote)}}function R(s,c){if(1&s&&(e.j41(0,"ion-list"),e.DNE(1,I,9,4,"ion-item",1),e.k0s()),2&s){const t=e.XpG();e.R7$(),e.Y8G("ngForOf",t.items)}}let G=(()=>{class s{navCtrl;backendService;constructor(t,n){this.navCtrl=t,this.backendService=n}items;itemTapped(t,n){this.navCtrl.navigateForward(this.backendService.loggedIn?"wertung-editor/"+n.id:`athlet-view/${this.backendService.competition}/${n.id}`)}static \u0275fac=function(n){return new(n||s)(e.rXU(f.q9),e.rXU(v.m))};static \u0275cmp=e.VBU({type:s,selectors:[["riege-list"]],inputs:{items:"items"},decls:1,vars:1,consts:[[4,"ngIf"],[3,"click",4,"ngFor","ngForOf"],[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end","class","result-value",4,"ngIf"],["slot","end","class","result-no-value",4,"ngIf"],["slot","end",1,"result-value"],["slot","end",1,"result-no-value"]],template:function(n,i){1&n&&e.DNE(0,R,2,1,"ion-list",0),2&n&&e.Y8G("ngIf",i.items&&i.items.length>0)},dependencies:[d.Sq,d.bT,r.mC,r.uz,r.he,r.nf,r.JI,d.QX],styles:[".result-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-success)}.result-no-value[_ngcontent-%COMP%]{font-size:large;font-weight:700;margin-top:17px;margin-left:0;padding-left:0;padding-top:0;color:var(--ion-color-warning)}"]})}return s})();function F(s,c){1&s&&(e.j41(0,"div")(1,"ion-title"),e.EFF(2,"Resultate"),e.k0s()())}function j(s,c){if(1&s&&(e.j41(0,"div")(1,"ion-title"),e.EFF(2,"Resultaterfassung"),e.j41(3,"div",14),e.EFF(4),e.k0s()()()),2&s){const t=e.XpG();e.R7$(3),e.Y8G("ngClass",t.durchgangstateClass),e.R7$(),e.JRh(t.durchgangstate())}}function T(s,c){if(1&s&&(e.nrm(0,"riege-list",15),e.nI1(1,"async")),2&s){const t=e.XpG();e.Y8G("items",e.bMT(1,1,t.getWertungen()))}}function $(s,c){1&s&&e.nrm(0,"ion-icon",16)}function w(s,c){if(1&s){const t=e.RV6();e.j41(0,"ion-button",17),e.bIt("click",function(){e.eBV(t);const i=e.XpG();return e.Njj(i.finish())}),e.nrm(1,"ion-icon",18),e.EFF(2," Eingaben abschliessen "),e.k0s()}if(2&s){const t=e.XpG();e.Y8G("disabled",!(t.geraet&&t.getSteps()))}}let N=(()=>{class s{navCtrl;backendService;alertCtrl;zone;durchgangopen=!1;geraete=[];constructor(t,n,i,a){this.navCtrl=t,this.backendService=n,this.alertCtrl=i,this.zone=a,this.backendService.durchgangStarted.pipe((0,k.T)(g=>g.filter(l=>(0,m.MB)(l.durchgang)===(0,m.MB)(this.backendService.durchgang)&&l.wettkampfUUID===this.backendService.competition).length>0)).subscribe(g=>{this.durchgangopen=g})}ionViewWillEnter(){this.geraete.length>0&&!this.backendService.captionmode&&this.backendService.activateCaptionMode()}ngOnInit(){this.backendService.geraeteSubject.subscribe(t=>{this.backendService.captionmode&&(this.geraete=t)})}durchgangstate(){return this.backendService.isWebsocketConnected()?this.durchgangopen?"gestartet":"gesperrt":"offline"}get durchgangstateClass(){return{open:this.backendService.isWebsocketConnected()&&this.durchgangopen,closed:this.backendService.isWebsocketConnected()&&!this.durchgangopen,offline:!this.backendService.isWebsocketConnected()}}set competition(t){this.stationFreezed||this.backendService.getDurchgaenge(t)}get competition(){return this.backendService.competition}set durchgang(t){this.stationFreezed||this.backendService.getGeraete(this.competition,t)}get durchgang(){return this.backendService.durchgang}set geraet(t){this.stationFreezed||this.backendService.getSteps(this.competition,this.durchgang,t)}get geraet(){return this.backendService.geraet}set step(t){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,t)}get step(){return this.backendService.step}get stationFreezed(){return this.backendService.stationFreezed}isLoggedIn(){return this.backendService.loggedIn}get nextStepCaption(){return this.isLoggedIn()?"N\xe4chste Riege":"N\xe4chstes Ger\xe4t"}get prevStepCaption(){return this.isLoggedIn()?"Vorherige Riege":"Vorheriges Ger\xe4t"}itemTapped(t){t.getOpenAmount().then(n=>{n>10||n<-10?t.close():t.open("end")})}nextStep(t){this.backendService.nextGeraet().subscribe(n=>{this.step=n,t.close()})}prevStep(t){this.backendService.prevGeraet().subscribe(n=>{this.step=n,t.close()})}get station(){return this.isLoggedIn()?this.geraetName()+" - "+this.step+". Riege von "+this.getSteps()?.length+", "+this.durchgang:this.geraetName()+" - "+this.step+". Ger\xe4t von "+this.getGeraete()?.length+", "+this.durchgang}get previousGearImage(){return this.isLoggedIn()?void 0:p.ec[this.backendService.getPrevGeraet()]||void 0}get nextGearImage(){return this.isLoggedIn()?void 0:p.ec[this.backendService.getNextGeraet()]||void 0}get gearImage(){return p.ec[this.backendService.geraet]||void 0}showCompleteAlert(){var t=this;return(0,u.A)(function*(){const n=t.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+t.station+'" k\xf6nnen die erfassten Wertungen nur noch im Wettkampf-B\xfcro korrigiert werden.',buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{const i=n.then(a=>a.dismiss());return t.zone.run(()=>{t.backendService.finishStation(t.competition,t.durchgang,t.geraet,t.step).subscribe(a=>{0===a.length&&i.then(()=>{t.navCtrl.pop()})})}),!1}}]});n.then(i=>i.present())})()}toastMissingResult(t){var n=this;return(0,u.A)(function*(){const i=t[0].vorname.toUpperCase()+" "+t[0].name.toUpperCase()+" ("+t[0].verein+")";(yield n.alertCtrl.create({header:"Achtung",subHeader:"Fehlendes Resultat!",message:"In dieser Riege gibt es noch "+t.length+" leere Wertungen! - Bitte pr\xfcfen, ob "+i+" geturnt hat.",buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{}},{text:i+" hat nicht geturnt",role:"edit",handler:()=>{t.splice(0,1),t.length>0?n.toastMissingResult(t):n.showCompleteAlert()}},{text:"Korrigieren",role:"edit",handler:()=>{n.navCtrl.navigateForward("wertung-editor/"+t[0].id)}}]})).present()})()}finish(){const t=this.backendService.wertungen.filter(n=>void 0===n.wertung.endnote);0!==t.length?this.toastMissingResult(t):this.showCompleteAlert()}getCompetitions(){return this.backendService.competitions}competitionName(){if(!this.backendService.competitions)return"";const t=this.backendService.competitions.filter(n=>n.uuid===this.backendService.competition).map(n=>n.titel+"<br>"+(n.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===t.length?t[0]:""}geraetName(){if(!this.geraete||0===this.geraete.length)return"";const t=this.geraete.filter(n=>n.id===this.backendService.geraet).map(n=>n.name);return 1===t.length?t[0]:""}getDurchgaenge(){return this.backendService.durchgaenge}getGeraete(){return this.geraete||[]}getSteps(){return this.backendService.steps}getWertungen(){return this.backendService.wertungenSubject}static \u0275fac=function(n){return new(n||s)(e.rXU(f.q9),e.rXU(v.m),e.rXU(r.hG),e.rXU(e.SKi))};static \u0275cmp=e.VBU({type:s,selectors:[["app-station"]],decls:29,vars:19,consts:[["slidingStepItem",""],["slot","start"],["defaultHref","/"],[4,"ngIf"],["slot","end",1,"ion-padding-end",3,"innerHtml"],[3,"items",4,"ngIf"],[3,"click"],["slot","start",3,"src"],["slot","end","name","arrow-forward-circle-outline",4,"ngIf"],["side","start"],["color","primary",3,"click","disabled"],[3,"src"],["side","end"],["size","large","expand","block","color","success",3,"disabled","click",4,"ngIf"],[1,"stateinfo",3,"ngClass"],[3,"items"],["slot","end","name","arrow-forward-circle-outline"],["size","large","expand","block","color","success",3,"click","disabled"],["slot","start","name","git-commit"]],template:function(n,i){if(1&n){const a=e.RV6();e.j41(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1),e.nrm(3,"ion-back-button",2),e.k0s(),e.DNE(4,F,3,0,"div",3)(5,j,5,2,"div",3),e.nrm(6,"ion-note",4),e.k0s()(),e.j41(7,"ion-content"),e.DNE(8,T,2,3,"riege-list",5),e.nI1(9,"async"),e.k0s(),e.j41(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list")(13,"ion-item-sliding",null,0)(15,"ion-item",6),e.bIt("click",function(){e.eBV(a);const l=e.sdS(14);return e.Njj(i.itemTapped(l))}),e.nrm(16,"ion-icon",7),e.j41(17,"ion-label"),e.EFF(18),e.k0s(),e.DNE(19,$,1,0,"ion-icon",8),e.k0s(),e.j41(20,"ion-item-options",9)(21,"ion-item-option",10),e.bIt("click",function(){e.eBV(a);const l=e.sdS(14);return e.Njj(i.prevStep(l))}),e.nrm(22,"ion-icon",11),e.EFF(23),e.k0s()(),e.j41(24,"ion-item-options",12)(25,"ion-item-option",10),e.bIt("click",function(){e.eBV(a);const l=e.sdS(14);return e.Njj(i.nextStep(l))}),e.nrm(26,"ion-icon",11),e.EFF(27),e.k0s()()(),e.DNE(28,w,3,1,"ion-button",13),e.k0s()()()}2&n&&(e.R7$(4),e.Y8G("ngIf",!i.isLoggedIn()),e.R7$(),e.Y8G("ngIf",i.isLoggedIn()),e.R7$(),e.Y8G("innerHtml",i.competitionName(),e.npT),e.R7$(2),e.Y8G("ngIf",i.geraet&&i.durchgang&&i.step&&e.bMT(9,17,i.getWertungen())),e.R7$(8),e.Mz_("src","assets/imgs/",i.gearImage,""),e.R7$(2),e.JRh(i.station),e.R7$(),e.Y8G("ngIf",i.geraet&&i.getSteps()),e.R7$(2),e.Y8G("disabled",!i.geraet||!i.getSteps()),e.R7$(),e.Mz_("src","assets/imgs/",i.previousGearImage,""),e.R7$(),e.SpI(" ",i.prevStepCaption," "),e.R7$(2),e.Y8G("disabled",!i.geraet||!i.getSteps()),e.R7$(),e.Mz_("src","assets/imgs/",i.nextGearImage,""),e.R7$(),e.SpI(" ",i.nextStepCaption," "),e.R7$(),e.Y8G("ngIf",i.isLoggedIn()&&i.durchgangopen))},dependencies:[d.YU,d.bT,r.Jm,r.QW,r.W9,r.M0,r.eU,r.iq,r.uz,r.LU,r.CE,r.A7,r.he,r.nf,r.JI,r.BC,r.ai,r.el,G,d.Jj],styles:[".stateinfo.closed[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-danger)}.stateinfo.open[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-success)}.stateinfo.offline[_ngcontent-%COMP%]{padding-left:5px;border-left:6px solid var(--ion-color-warning)}ion-icon[_ngcontent-%COMP%]{color:var(--ion-card-color);font-size:35px;border-color:var(--ion-item-border-color);border-width:2px;border-style:solid;border-radius:4px;margin-right:16px}"]})}return s})();var E=o(5079);const M=[{path:"",component:N}];let P=(()=>{class s{static \u0275fac=function(n){return new(n||s)};static \u0275mod=e.$C({type:s});static \u0275inj=e.G2t({imports:[d.MD,b.YN,r.bv,S.iI.forChild(M),E.h]})}return s})()}}]);