<template>
    <div>
        <Camera :resolution="{ width: 375, height: 600 }" ref="camera" autoplay></Camera>
        <Buttonbar :defaultSelected="selectedButtonName" @update:selectedButton="updateSelectedButton" />
        <audio v-if="audioUrl" :src="audioUrl" autoplay style="display: none;"></audio>
    </div>
</template>

<script setup lang="ts">
import Camera from 'simple-vue-camera';

const camera = ref<InstanceType<typeof Camera>>();
const snapshotUrl = ref<string | null>(null);
const selectedButtonName = ref<string>('Text');

interface JsonResponse {
    audio_path?: string;
    [key: string]: any;
}

const jsonResponse = ref<JsonResponse | null>(null);
const audioUrl = ref<string | null>(null);

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
        snapshotUrl.value = URL.createObjectURL(blob);
        sendImageToEndpoint(blob);
    }
};

const sendImageToEndpoint = async (blob: Blob) => {
    const endpoint = endpoints[selectedButtonName.value];
    if (!endpoint) return;

    const formData = new FormData();
    formData.append('file', blob);

    try {
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
        console.log('JSON Response:', jsonResponse.value);

        const audioPath = jsonResponse.value?.audio_path;
        if (audioPath) {
            const encodedAudioPath = encodeURIComponent(audioPath);
            const audioFileUrl = `http://112.137.129.161:8000/download_audio?audio_path=${encodedAudioPath}`;
            console.log('Audio File URL:', audioFileUrl);
            audioUrl.value = audioFileUrl;
        }
    } catch (error) {
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

let intervalId: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
    intervalId = setInterval(snapshot, 5000);
});

onUnmounted(() => {
    if (intervalId) {
        clearInterval(intervalId);
    }
});
</script>

<style scoped></style>