<template>
    <div class="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 to-purple-100">
        <header class="flex items-center justify-between px-6 py-4 shadow-md bg-white/80 backdrop-blur sticky top-0 z-10">
            <div class="flex items-center gap-2">
                <button @click="showSettingsDialog = true" aria-label="Open settings" class="rounded-full p-2 hover:bg-blue-100 transition">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="h-6 w-6 text-blue-700">
                        <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                </button>
                <span class="font-bold text-2xl text-blue-700 tracking-tight">Vision Mate</span>
            </div>
            <button @click="showAboutDialog = true" aria-label="About" class="rounded-full p-2 hover:bg-purple-100 transition">
                <i class="fa-regular fa-circle-question text-2xl text-purple-700"></i>
            </button>
        </header>

        <main class="flex flex-col flex-grow items-center justify-center gap-6 py-6">
            <div class="w-full max-w-md aspect-[3/5] rounded-2xl overflow-hidden shadow-lg border-2 border-blue-200 bg-white/70 flex items-center justify-center">
                <Camera :resolution="cameraResolution" ref="camera" autoplay class="w-full h-full object-cover" />
            </div>

            <div class="w-full max-w-xl">
                <Buttonbar :defaultSelected="selectedButtonName" @update:selectedButton="updateSelectedButton" class="w-full" />
            </div>
        </main>

        <audio v-if="audioUrl" :src="audioUrl" autoplay style="display: none;"></audio>

        <!-- Settings Dialog -->
        <dialog ref="settingsDialog" class="modal" :open="showSettingsDialog">
            <div class="modal-box bg-white rounded-xl shadow-xl">
                <h3 class="text-lg font-bold text-blue-700">Settings</h3>
                <p class="py-4 text-gray-600">Settings content goes here.</p>
            </div>
            <form method="dialog" class="modal-backdrop">
                <button @click.prevent="showSettingsDialog = false" class="btn btn-sm btn-outline mt-2">Close</button>
            </form>
        </dialog>

        <!-- About Dialog -->
        <dialog ref="aboutDialog" class="modal" :open="showAboutDialog">
            <div class="modal-box bg-white rounded-xl shadow-xl">
                <h3 class="text-lg font-bold text-purple-700">About</h3>
                <p class="py-4 text-gray-600">Vision Mate helps you recognize text, currency, objects, products, distances, and faces using your camera. Powered by AI.</p>
            </div>
            <form method="dialog" class="modal-backdrop">
                <button @click.prevent="showAboutDialog = false" class="btn btn-sm btn-outline mt-2">Close</button>
            </form>
        </dialog>

        <div v-if="errorMessage" class="alert alert-error mt-4 mx-auto max-w-md">{{ errorMessage }}</div>
    </div>
</template>

<script setup lang="ts">
import Camera from 'simple-vue-camera';
import Buttonbar from '~/components/Buttonbar.vue';
import { ref, onMounted, onUnmounted } from 'vue';

const camera = ref<InstanceType<typeof Camera>>();
const selectedButtonName = ref<string>('Text');
const cameraResolution = ref<{ width: number, height: number }>({ width: 375, height: 600 });

interface JsonResponse {
    audio_path?: string;
    [key: string]: any;
}

const jsonResponse = ref<JsonResponse | null>(null);
const audioUrl = ref<string | null>(null);
const errorMessage = ref<string | null>(null);

const showSettingsDialog = ref(false);
const showAboutDialog = ref(false);

const endpoints: Record<string, string> = {
    'Text': '/document_recognition',
    'Currency': '/currency_detection',
    'Object': '/image_captioning',
    'Product': '/product_recognition',
    'Distance': '/distance_estimate',
    'Face': '/face_detection/recognize',
};

const snapshot = async () => {
    const blob = await camera.value?.snapshot();
    if (blob) {
        sendImageToEndpoint(blob);
    }
};

const sendImageToEndpoint = async (blob: Blob) => {
    const endpoint = endpoints[selectedButtonName.value];
    if (!endpoint) return;

    const formData = new FormData();
    formData.append('file', blob);

    try {
        errorMessage.value = null;
        const response = await fetch(endpoint, {
            method: 'POST',
            body: formData,
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        jsonResponse.value = await response.json();
        const audioPath = jsonResponse.value?.audio_path;
        if (audioPath) {
            const encodedAudioPath = encodeURIComponent(audioPath);
            // Use relative path for proxy
            const audioFileUrl = `/download_audio?audio_path=${encodedAudioPath}`;
            audioUrl.value = audioFileUrl;
        }
    } catch (error: any) {
        errorMessage.value = 'Failed to process image. Please try again.';
        console.error('Error sending image to endpoint:', error);
    }
};

const playButtonText = (text: string) => {
    const utterance = new SpeechSynthesisUtterance(text);
    speechSynthesis.speak(utterance);
};

const updateSelectedButton = (buttonName: string) => {
    selectedButtonName.value = buttonName;
    playButtonText(buttonName); 
};

const updateCameraResolution = () => {
    if (window.innerWidth > window.innerHeight) {
        cameraResolution.value = { width: 600, height: 375 };
    } else {
        cameraResolution.value = { width: 375, height: 600 };
    }
};

let intervalId: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
    updateCameraResolution();
    window.addEventListener('resize', updateCameraResolution);
    intervalId = setInterval(snapshot, 5000);
});

onUnmounted(() => {
    if (intervalId) {
        clearInterval(intervalId);
    }
    window.removeEventListener('resize', updateCameraResolution);
});
</script>

<style scoped>
body {
  background: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 100%);
}
.modal-box {
  max-width: 90vw;
}
</style>