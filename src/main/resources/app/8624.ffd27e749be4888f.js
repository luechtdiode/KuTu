"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8624],{8624:(M,_,r)=>{r.r(_),r.d(_,{HomePageModule:()=>D});var l=r(177),g=r(4341),s=r(7125),m=r(1313),p=r(3537),u=r(6354),e=r(4020),h=r(2872),k=r(2350);function f(n,a){if(1&n&&(e.j41(0,"ion-label"),e.EFF(1),e.k0s()),2&n){const t=e.XpG(2);e.R7$(),e.Lme(" ",t.durchgang,", ",t.geraetName()," ")}}function b(n,a){if(1&n&&(e.j41(0,"ion-note",15),e.EFF(1),e.k0s()),2&n){const t=e.XpG(2);e.R7$(),e.SpI(" ","Riege "+t.step," ")}}function S(n,a){1&n&&e.nrm(0,"ion-icon",16)}function v(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-list")(1,"ion-item")(2,"ion-label"),e.EFF(3),e.k0s()(),e.j41(4,"ion-item"),e.DNE(5,f,2,2,"ion-label",5),e.k0s(),e.j41(6,"ion-item-sliding",null,0)(8,"ion-item",7),e.bIt("click",function(){e.eBV(t);const i=e.sdS(7),c=e.XpG();return e.Njj(c.itemTapped(i))}),e.j41(9,"ion-label"),e.EFF(10),e.k0s(),e.DNE(11,b,2,1,"ion-note",8)(12,S,1,0,"ion-icon",9),e.k0s(),e.j41(13,"ion-item-options",10)(14,"ion-item-option",11),e.bIt("click",function(){e.eBV(t);const i=e.sdS(7),c=e.XpG();return e.Njj(c.prevStep(i))}),e.nrm(15,"ion-icon",12),e.EFF(16," Vorherige Riege "),e.k0s()(),e.j41(17,"ion-item-options",13)(18,"ion-item-option",11),e.bIt("click",function(){e.eBV(t);const i=e.sdS(7),c=e.XpG();return e.Njj(c.nextStep(i))}),e.nrm(19,"ion-icon",14),e.EFF(20," N\xe4chste Riege "),e.k0s()()()()}if(2&n){const t=e.XpG();e.R7$(3),e.JRh(t.competitionName()),e.R7$(2),e.Y8G("ngIf",t.durchgang&&t.geraet),e.R7$(5),e.JRh(t.geraet&&t.getSteps()?"Riege ("+t.getSteps()[0]+" bis "+t.getSteps()[t.getSteps().length-1]+")":"Keine Daten"),e.R7$(),e.Y8G("ngIf",t.geraet&&t.getSteps()&&t.getSteps().length>0),e.R7$(),e.Y8G("ngIf",t.geraet&&t.getSteps()),e.R7$(2),e.Y8G("disabled",!t.geraet||!t.getSteps()),e.R7$(4),e.Y8G("disabled",!t.geraet||!t.getSteps())}}function R(n,a){if(1&n&&(e.j41(0,"ion-select-option",22),e.EFF(1),e.nI1(2,"date"),e.k0s()),2&n){const t=a.$implicit;e.Y8G("value",t.uuid),e.R7$(),e.SpI(" ",t.titel+" "+e.i5U(2,2,t.datum,"dd-MM-yy"),"")}}function x(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-select",20),e.mxI("ngModelChange",function(i){e.eBV(t);const c=e.XpG(2);return e.DH7(c.competition,i)||(c.competition=i),e.Njj(i)}),e.DNE(1,R,3,5,"ion-select-option",21),e.k0s()}if(2&n){const t=e.XpG(2);e.R50("ngModel",t.competition),e.R7$(),e.Y8G("ngForOf",t.getCompetitions())}}function G(n,a){if(1&n&&(e.j41(0,"ion-select-option",22),e.EFF(1),e.k0s()),2&n){const t=a.$implicit;e.Y8G("value",t),e.R7$(),e.JRh(t)}}function j(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-select",23),e.mxI("ngModelChange",function(i){e.eBV(t);const c=e.XpG(2);return e.DH7(c.durchgang,i)||(c.durchgang=i),e.Njj(i)}),e.DNE(1,G,2,2,"ion-select-option",21),e.k0s()}if(2&n){const t=e.XpG(2);e.R50("ngModel",t.durchgang),e.R7$(),e.Y8G("ngForOf",t.getDurchgaenge())}}function I(n,a){if(1&n&&(e.j41(0,"ion-select-option",22),e.EFF(1),e.k0s()),2&n){const t=a.$implicit;e.Y8G("value",t.id),e.R7$(),e.JRh(t.name)}}function T(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-select",24),e.mxI("ngModelChange",function(i){e.eBV(t);const c=e.XpG(2);return e.DH7(c.geraet,i)||(c.geraet=i),e.Njj(i)}),e.DNE(1,I,2,2,"ion-select-option",21),e.k0s()}if(2&n){const t=e.XpG(2);e.R50("ngModel",t.geraet),e.R7$(),e.Y8G("ngForOf",t.getGeraete())}}function E(n,a){if(1&n&&(e.j41(0,"ion-note",15),e.EFF(1),e.k0s()),2&n){const t=e.XpG(2);e.R7$(),e.SpI(" ","Riege "+t.step," ")}}function F(n,a){1&n&&e.nrm(0,"ion-icon",16)}function N(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-list")(1,"ion-item"),e.DNE(2,x,2,2,"ion-select",17),e.k0s(),e.j41(3,"ion-item"),e.DNE(4,j,2,2,"ion-select",18),e.k0s(),e.j41(5,"ion-item"),e.DNE(6,T,2,2,"ion-select",19),e.k0s(),e.j41(7,"ion-item-sliding",null,0)(9,"ion-item",7),e.bIt("click",function(){e.eBV(t);const i=e.sdS(8),c=e.XpG();return e.Njj(c.itemTapped(i))}),e.j41(10,"ion-label"),e.EFF(11),e.k0s(),e.DNE(12,E,2,1,"ion-note",8)(13,F,1,0,"ion-icon",9),e.k0s(),e.j41(14,"ion-item-options",10)(15,"ion-item-option",11),e.bIt("click",function(){e.eBV(t);const i=e.sdS(8),c=e.XpG();return e.Njj(c.prevStep(i))}),e.EFF(16," Vorherige Riege "),e.k0s()(),e.j41(17,"ion-item-options",13)(18,"ion-item-option",11),e.bIt("click",function(){e.eBV(t);const i=e.sdS(8),c=e.XpG();return e.Njj(c.nextStep(i))}),e.EFF(19," N\xe4chste Riege "),e.k0s()()()()}if(2&n){const t=e.XpG();e.R7$(2),e.Y8G("ngIf",t.getCompetitions()),e.R7$(2),e.Y8G("ngIf",t.competition&&t.getDurchgaenge()&&t.getDurchgaenge().length>0),e.R7$(2),e.Y8G("ngIf",t.durchgang&&t.getGeraete()&&t.getGeraete().length>0),e.R7$(5),e.JRh(t.geraet&&t.getSteps()?"Riege ("+t.getSteps()[0]+" bis "+t.getSteps()[t.getSteps().length-1]+")":"Keine Daten"),e.R7$(),e.Y8G("ngIf",t.geraet&&t.getSteps()&&t.getSteps().length>0),e.R7$(),e.Y8G("ngIf",t.geraet&&t.getSteps()),e.R7$(2),e.Y8G("disabled",!t.geraet||!t.getSteps()),e.R7$(3),e.Y8G("disabled",!t.geraet||!t.getSteps())}}function H(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-button",25),e.bIt("click",function(){e.eBV(t);const i=e.XpG();return e.Njj(i.navToStation())}),e.nrm(1,"ion-icon",26),e.EFF(2," Resultate "),e.k0s()}if(2&n){const t=e.XpG();e.Y8G("disabled",!(t.geraet&&t.getSteps()))}}function C(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-button",25),e.bIt("click",function(){e.eBV(t);const i=e.XpG();return e.Njj(i.navToStation())}),e.nrm(1,"ion-icon",27),e.EFF(2," Resultate "),e.k0s()}if(2&n){const t=e.XpG();e.Y8G("disabled",!(t.geraet&&t.getSteps()))}}function P(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-list")(1,"ion-button",28),e.bIt("click",function(){e.eBV(t);const i=e.XpG();return e.Njj(i.unlock())}),e.nrm(2,"ion-icon",29),e.EFF(3,"Einstellung zur\xfccksetzen "),e.k0s()()}}function Y(n,a){if(1&n){const t=e.RV6();e.j41(0,"ion-list")(1,"ion-button",30),e.bIt("click",function(){e.eBV(t);const i=e.XpG();return e.Njj(i.logout())}),e.nrm(2,"ion-icon",31),e.EFF(3,"Abmelden "),e.k0s()()}}let B=(()=>{class n{navCtrl;backendService;alertCtrl;durchgangstate;durchgangopen=!1;constructor(t,o,i){this.navCtrl=t,this.backendService=o,this.alertCtrl=i,this.backendService.getCompetitions(),this.backendService.durchgangStarted.pipe((0,u.T)(c=>c.filter(d=>(0,p.MB)(d.durchgang)===(0,p.MB)(this.backendService.durchgang)&&d.wettkampfUUID===this.backendService.competition).length>0)).subscribe(c=>{this.durchgangstate=c?"gestartet":"gesperrt",this.durchgangopen=c})}ngOnInit(){this.backendService.competition&&this.backendService.getDurchgaenge(this.backendService.competition)}get isLocked(){return this.backendService.stationFreezed}get isOpenAndActive(){return this.durchgangopen&&this.backendService.isWebsocketConnected()}get isLoggedIn(){return this.backendService.loggedIn}set competition(t){this.isLocked||this.backendService.getDurchgaenge(t)}get competition(){return this.backendService.competition||""}set durchgang(t){this.isLocked||this.backendService.getGeraete(this.competition,t)}get durchgang(){return this.backendService.durchgang||""}set geraet(t){this.isLocked||this.backendService.getSteps(this.competition,this.durchgang,t)}get geraet(){return this.backendService.geraet||-1}set step(t){this.backendService.getWertungen(this.competition,this.durchgang,this.geraet,t)}get step(){return this.backendService.step||-1}itemTapped(t){t.getOpenAmount().then(o=>{o>10||o<-10?t.close():t.open("end")})}nextStep(t){this.step=this.backendService.nextStep(),t.close()}prevStep(t){this.step=this.backendService.prevStep(),t.close()}getCompetitions(){return this.backendService.competitions}competitionName(){if(!this.backendService.competitions)return"";const t=this.backendService.competitions.filter(o=>o.uuid===this.backendService.competition).map(o=>o.titel+", am "+(o.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===t.length?t[0]:""}geraetName(){if(!this.backendService.geraete)return"";const t=this.backendService.geraete.filter(o=>o.id===this.backendService.geraet).map(o=>o.name);return 1===t.length?t[0]:""}getDurchgaenge(){return this.backendService.durchgaenge}getGeraete(){return this.backendService.geraete}getSteps(){return!this.backendService.steps&&this.backendService.geraet&&this.backendService.getSteps(this.competition,this.durchgang,this.geraet),this.backendService.steps}navToStation(){this.navCtrl.navigateForward("station")}unlock(){const t=this.competitionName()+"/"+this.durchgang+"/"+this.geraetName();this.alertCtrl.create({header:"Info",message:'Die Fixierung auf die Station "'+t+'" wird aufgehoben.<br>Du kannst dann die Station frei einstellen.',buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.unlock()}}]}).then(i=>i.present())}logout(){this.alertCtrl.create({header:"Achtung",subHeader:"Nach dem Abmelden k\xf6nnen keine weiteren Resultate mehr erfasst werden.",message:"F\xfcr die Erfassung weiterer Resultate wird eine Neuanmeldung erforderlich.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.logout()}}]}).then(o=>o.present())}finish(){const t=this.geraetName()+"/Riege #"+this.step,o=this.alertCtrl.create({header:"Achtung",message:'Nach dem Abschliessen der Station "'+t+'" k\xf6nnen die erfassten Wertungen nur noch im Wettkampf-B\xfcro korrigiert werden.',buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{const i=o.then(c=>c.dismiss());return this.backendService.finishStation(this.competition,this.durchgang,this.geraet,this.step).subscribe(c=>{0===c.length&&i.then(()=>{this.navCtrl.pop()})}),!1}}]});o.then(i=>i.present())}static \u0275fac=function(o){return new(o||n)(e.rXU(h.q9),e.rXU(k.m),e.rXU(s.hG))};static \u0275cmp=e.VBU({type:n,selectors:[["app-page-home"]],standalone:!1,decls:18,vars:8,consts:[["slidingStepItem",""],[3,"translucent"],["slot","start"],["slot","icon-only","name","menu"],[3,"fullscreen"],[4,"ngIf"],["size","large","expand","block","color","primary",3,"disabled","click",4,"ngIf"],[3,"click"],["slot","end",4,"ngIf"],["slot","end","name","arrow-forward-circle-outline",4,"ngIf"],["side","start"],["color","secondary",3,"click","disabled"],["name","arrow-back-circle-outline"],["side","end"],["name","arrow-forward-circle-outline"],["slot","end"],["slot","end","name","arrow-forward-circle-outline"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Durchgang","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Ger\xe4t","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["label","Durchgang","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],["label","Ger\xe4t","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],["size","large","expand","block","color","primary",3,"click","disabled"],["slot","start","name","list"],["slot","start","name","create"],["size","large","expand","block","color","warning",3,"click"],["slot","start","name","unlock"],["size","large","expand","block","color","danger",3,"click"],["slot","start","name","log-out"]],template:function(o,i){1&o&&(e.j41(0,"ion-header",1)(1,"ion-toolbar")(2,"ion-buttons",2)(3,"ion-menu-toggle")(4,"ion-button"),e.nrm(5,"ion-icon",3),e.k0s()()(),e.j41(6,"ion-title"),e.EFF(7,"Wettkampf App"),e.k0s()()(),e.j41(8,"ion-content",4),e.DNE(9,v,21,7,"ion-list",5)(10,N,20,8,"ion-list",5),e.j41(11,"ion-list"),e.DNE(12,H,3,1,"ion-button",6)(13,C,3,1,"ion-button",6),e.k0s()(),e.j41(14,"ion-footer")(15,"ion-toolbar"),e.DNE(16,P,4,0,"ion-list",5)(17,Y,4,0,"ion-list",5),e.k0s()()),2&o&&(e.Y8G("translucent",!0),e.R7$(8),e.Y8G("fullscreen",!0),e.R7$(),e.Y8G("ngIf",i.isLocked),e.R7$(),e.Y8G("ngIf",!i.isLocked),e.R7$(2),e.Y8G("ngIf",!i.isLoggedIn),e.R7$(),e.Y8G("ngIf",i.isLoggedIn),e.R7$(3),e.Y8G("ngIf",i.isLocked),e.R7$(),e.Y8G("ngIf",i.isLoggedIn))},dependencies:[l.Sq,l.bT,g.BC,g.vS,s.Jm,s.QW,s.W9,s.M0,s.eU,s.iq,s.uz,s.LU,s.CE,s.A7,s.he,s.nf,s.cA,s.JI,s.Nm,s.Ip,s.BC,s.ai,s.Je,l.vh],encapsulation:2})}return n})(),D=(()=>{class n{static \u0275fac=function(o){return new(o||n)};static \u0275mod=e.$C({type:n});static \u0275inj=e.G2t({imports:[l.MD,g.YN,s.bv,m.iI.forChild([{path:"",component:B}])]})}return n})()}}]);